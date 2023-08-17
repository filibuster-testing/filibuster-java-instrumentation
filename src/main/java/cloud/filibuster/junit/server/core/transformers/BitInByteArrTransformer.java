package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.reflect.TypeToken;

public final class BitInByteArrTransformer implements Transformer<byte[], List<SimpleImmutableEntry<Integer, Integer>>> {
    private boolean hasNext = true;
    private byte[] result;
    private Accumulator<byte[], List<SimpleImmutableEntry<Integer, Integer>>> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BitInByteArrTransformer transform(byte[] payload, Accumulator<byte[], List<SimpleImmutableEntry<Integer, Integer>>> accumulator) {

        // The context saved in the accumulator is an array of SimpleImmutableEntries, each has an integer key and an integer value.
        // Each SimpleImmutableEntry represents a byte/bit pair that has been mutated in the payload.
        // The key of the entry is the idx of the byte in the payload, and the value is the idx of the bit in the byte.
        List<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

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
            throw new TransformerNullResultException("Result is null. getResult() was probably called before transform()!");
        }
        return this.result;
    }

    @Override
    public Accumulator<byte[], List<SimpleImmutableEntry<Integer, Integer>>> getInitialAccumulator(byte[] referenceValue) {
        Accumulator<byte[], List<SimpleImmutableEntry<Integer, Integer>>> accumulator = new Accumulator<>();
        List<SimpleImmutableEntry<Integer, Integer>> ctx = new ArrayList<>();
        // Initial entry for byte and bit 0
        ctx.add(new SimpleImmutableEntry<>(0, 0));
        accumulator.setContext(ctx);
        this.result = referenceValue;
        accumulator.setReferenceValue(referenceValue);
        return accumulator;
    }

    @Override
    public Accumulator<byte[], List<SimpleImmutableEntry<Integer, Integer>>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            List<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

            SimpleImmutableEntry<Integer, Integer> newEntry = getNextEntry(ctx.get(ctx.size() - 1));
            ctx.add(newEntry);

            accumulator.setContext(ctx);
            return accumulator;
        }
    }

    @Override
    public Type getAccumulatorType() {
        Type byteArrType = TypeToken.get(byte[].class).getType();
        Type simpleEntryType = TypeToken.getParameterized(SimpleImmutableEntry.class, Integer.class, Integer.class).getType();
        Type listType = TypeToken.getParameterized(List.class, simpleEntryType).getType();

        return TypeToken.getParameterized(
                Accumulator.class,
                byteArrType,
                listType).getType();
    }

    private static SimpleImmutableEntry<Integer, Integer> getNextEntry(SimpleImmutableEntry<Integer, Integer> ctx) {
        // There are only 8 bits in a byte
        int byteIdx = ctx.getValue() < 7 ? ctx.getKey() : ctx.getKey() + 1;
        int bitIdx = ctx.getValue() < 7 ? ctx.getValue() + 1 : 0;
        return new SimpleImmutableEntry<>(byteIdx, bitIdx);
    }

}
