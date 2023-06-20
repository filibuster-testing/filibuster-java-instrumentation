package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Random;

public final class BitInByteArrTransformer implements Transformer<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;
    private Byte[] result;
    private Accumulator<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BitInByteArrTransformer transform(Byte[] payload, Accumulator<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator) {
        ArrayList<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

        SimpleImmutableEntry<Integer, Integer> lastEntry = ctx.get(ctx.size() - 1);

        byte myByte = payload[lastEntry.getKey()];

        String myBits = String.format("%8s", Integer.toBinaryString(myByte & 0xFF)).replace(' ', '0');

        StringBuilder myMutatedBits = new StringBuilder(myBits);
        if (myMutatedBits.charAt(lastEntry.getValue()) == '0') {
            myMutatedBits.setCharAt(lastEntry.getValue(), '1');
        } else {
            myMutatedBits.setCharAt(lastEntry.getValue(), '0');
        }

        payload[lastEntry.getKey()] = Byte.parseByte(myMutatedBits.toString(), 2);

        this.result = payload;
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
    public Class<Byte[]> getPayloadType() {
        return Byte[].class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<ArrayList<SimpleImmutableEntry<Integer, Integer>>> getContextType() {
        return (Class<ArrayList<SimpleImmutableEntry<Integer, Integer>>>) (Class<?>) ArrayList.class;
    }

    @Override
    public Byte[] getResult() {
        if (this.result == null) {
            throw new FilibusterFaultInjectionException("getResult() called before transform()!");
        }
        return this.result;
    }

    @Override
    public Accumulator<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> getAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        }
        return this.accumulator;
    }

    @Override
    public Accumulator<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> getInitialAccumulator() {
        Accumulator<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> accumulator = new Accumulator<>();
        accumulator.setContext(new ArrayList<>());
        return accumulator;
    }

    @Override
    public Accumulator<Byte[], ArrayList<SimpleImmutableEntry<Integer, Integer>>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            ArrayList<SimpleImmutableEntry<Integer, Integer>> ctx = accumulator.getContext();

            int byteIdx, bitIdx;
            SimpleImmutableEntry<Integer, Integer> newEntry;
            do {
                byteIdx = generateRandomIdx(accumulator.getReferenceValue().length);
                bitIdx = new Random().nextInt(8);
                newEntry = new SimpleImmutableEntry<>(byteIdx, bitIdx);
                if (!ctx.contains(newEntry)) {
                    ctx.add(newEntry);
                    break;
                }
            } while (ctx.contains(newEntry));

            accumulator.setContext(ctx);
            return accumulator;
        }
    }

    private static int generateRandomIdx(int bound) {
        return rand.nextInt(bound);
    }

}
