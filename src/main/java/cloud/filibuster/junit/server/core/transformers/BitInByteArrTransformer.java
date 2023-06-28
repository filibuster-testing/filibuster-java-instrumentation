package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Random;

import com.google.gson.reflect.TypeToken;

import static cloud.filibuster.instrumentation.helpers.Property.getRandomSeedProperty;

public final class BitInByteArrTransformer implements Transformer<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> {
    private static final Random rand = new Random(getRandomSeedProperty());
    private boolean hasNext = true;
    private byte[] result;
    private Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BitInByteArrTransformer transform(byte[] payload, Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator) {

        // The context saved in the accumulator is an array of SimpleImmutableEntries, each has an integer key and an integer value.
        // Each SimpleImmutableEntry represents a byte/bit pair that has been mutated in the payload.
        // The key of the entry is the idx of the byte in the payload, and the value is the idx of the bit in the byte.
        ArrayList<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

        // The last entry in the context is the byte/bit pair that we want to mutate in this call.
        SimpleImmutableEntry<Integer, Integer> entryToMutate = ctx.get(ctx.size() - 1);  // Get the last entry in the context

        // Get the byte from the payload by its index
        byte myByte = payload[entryToMutate.getKey()];

        // Convert the byte to a string of bits:
        // 'myByte & 0xFF' performs a bitwise AND operation between myByte and 0xFF. This operation ensures that only the
        // lowest 8 bits of myByte are preserved, while any higher bits are set to zero.
        // 'Integer.toBinaryString' returns the representation of the integer argument as an unsigned integer in base 2.
        // 'String.format("%8s", ...)' formats the binary string with a width of 8 characters, padding it with spaces on the left if necessary.
        // 'String.replace' replaces the leading space characters with '0'.
        String myBits = String.format("%8s", Integer.toBinaryString(myByte & 0xFF)).replace(' ', '0');

        StringBuilder myMutatedBits = new StringBuilder(myBits);

        // If the bit is 0, mutate it to "1" or "-", depending on its position.
        if (myMutatedBits.charAt(entryToMutate.getValue()) == '0') {
            // Byte is signed. If the byte is positive (i.e., its first bit is 0), mutate it to "-".
            if (entryToMutate.getValue() == 0) {  // "entryToMutate.getValue" is the bit idx -> If the idx is 0, it's the first bit.
                myMutatedBits.setCharAt(0, '-');
            } else {
                // Otherwise, mutate the bit to 1.
                myMutatedBits.setCharAt(entryToMutate.getValue(), '1');
            }
        } else {
            // If the bit is 1, mutate it 0.
            myMutatedBits.setCharAt(entryToMutate.getValue(), '0');
        }

        // Based on the original payload, create a new byte array called mutatedPayload
        byte[] mutatedPayload = payload.clone();
        // Convert the mutated bits back to a byte and put it in the mutatedPayload array
        mutatedPayload[entryToMutate.getKey()] = (byte) (int) Integer.valueOf(myMutatedBits.toString(), 2);

        this.result = mutatedPayload;
        this.accumulator = accumulator;

        if (ctx.size() == payload.length * 8) {
            this.hasNext = false;
        }

        return this;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Type getPayloadType() {
        return byte[].class;
    }

    @Override
    public byte[] getResult() {
        if (this.result == null) {
            throw new FilibusterFaultInjectionException("getResult() called before transform()!");
        }
        return this.result;
    }

    @Override
    public Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> getInitialAccumulator() {
        Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator = new Accumulator<>();
        ArrayList<SimpleImmutableEntry<Integer, Integer>> ctx = new ArrayList<>();
        // Initial entry for byte 0 at a random bit - the byteBound is exclusive
        ctx.add(generateRandomEntry(1));
        accumulator.setContext(ctx);
        return accumulator;
    }

    @Override
    public Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            ArrayList<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

            SimpleImmutableEntry<Integer, Integer> newEntry;
            do {
                newEntry = generateRandomEntry(accumulator.getReferenceValue().length);
                if (!ctx.contains(newEntry)) {
                    ctx.add(newEntry);
                    break;
                }
            } while (ctx.contains(newEntry));

            accumulator.setContext(ctx);
            return accumulator;
        }
    }

    @Override
    public Type getAccumulatorType() {
        Type byteArrType = TypeToken.get(byte[].class).getType();
        Type simpleEntryType = TypeToken.getParameterized(SimpleImmutableEntry.class, Integer.class, Integer.class).getType();
        Type listType = TypeToken.getParameterized(ArrayList.class, simpleEntryType).getType();

        return TypeToken.getParameterized(
                Accumulator.class,
                byteArrType,
                listType).getType();
    }

    private static SimpleImmutableEntry<Integer, Integer> generateRandomEntry(int byteBound) {
        int byteIdx = generateRandomIdx(byteBound);
        int bitIdx = generateRandomIdx(8);  // There are only 8 bits in a byte
        return new SimpleImmutableEntry<>(byteIdx, bitIdx);
    }

    private static int generateRandomIdx(int bound) {
        return rand.nextInt(bound);
    }

}
