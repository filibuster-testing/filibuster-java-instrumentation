package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Random;

import com.google.gson.reflect.TypeToken;

public final class BitInByteArrTransformer implements Transformer<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;
    private byte[] result;
    private Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BitInByteArrTransformer transform(byte[] payload, Accumulator<byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator) {
        ArrayList<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

        SimpleImmutableEntry<Integer, Integer> entryToMutate = ctx.get(ctx.size() - 1);  // Get the last entry in the context

        byte mybyte = payload[entryToMutate.getKey()];

        String myBits = String.format("%8s", Integer.toBinaryString(mybyte & 0xFF)).replace(' ', '0');

        StringBuilder myMutatedBits = new StringBuilder(myBits);
        if (myMutatedBits.charAt(entryToMutate.getValue()) == '0') {
            if (entryToMutate.getValue() == 0) {  // byte is signed. If the byte is positive (i.e., its first bit is 0), mutate it to "-".
                myMutatedBits.setCharAt(0, '-');
            } else {
                myMutatedBits.setCharAt(entryToMutate.getValue(), '1');
            }
        } else {
            myMutatedBits.setCharAt(entryToMutate.getValue(), '0');
        }

        byte[] mutatedPayload = payload.clone();
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
