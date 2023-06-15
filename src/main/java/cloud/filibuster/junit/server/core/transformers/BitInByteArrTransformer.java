package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.HashMap;
import java.util.Random;

public final class BitInByteArrTransformer implements Transformer<Byte[], HashMap<Integer, Integer>> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;
    private Byte[] result;
    private Accumulator<Byte[], HashMap<Integer, Integer>> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BitInByteArrTransformer transform(Byte[] payload, Accumulator<Byte[], HashMap<Integer, Integer>> accumulator) {
        for (byte b : payload) {
            b = (byte) ~b;
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
    public Class<HashMap<Integer, Integer>> getContextType() {
        return (Class<HashMap<Integer, Integer>>) this.getClass()
                .getGenericSuperclass();
    }

    @Override
    public Byte[] getResult() {
        if (this.result == null) {
            throw new FilibusterFaultInjectionException("getResult() called before transform()!");
        }
        return this.result;
    }

    @Override
    public Accumulator<Byte[], HashMap<Integer, Integer>> getAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        }
        return this.accumulator;
    }

    @Override
    public Accumulator<Byte[], HashMap<Integer, Integer>> getInitialAccumulator() {
        Accumulator<Byte[], HashMap<Integer, Integer>> accumulator = new Accumulator<>();
        accumulator.setContext(new HashMap<>());
        return accumulator;
    }

    @Override
    public Accumulator<Byte[], HashMap<Integer, Integer>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            HashMap<Integer, Integer> entry = accumulator.getContext();

            int byteIdx = generateRandomIdx(accumulator.getReferenceValue().length);
            int bitIdx = new Random().nextInt(8);

            while (entry.containsKey(byteIdx) && entry.get(byteIdx) != bitIdx) {
                byteIdx = new Random().nextInt(accumulator.getReferenceValue().length);
                bitIdx = new Random().nextInt(8);
                if (entry.containsKey(byteIdx) && entry.get(byteIdx) == bitIdx) {
                    break;
                }
            }

            entry.put(byteIdx, bitIdx);
            accumulator.setContext(entry);
            return accumulator;
        }
    }

    private static int generateRandomIdx(int bound) {
        return new Random().nextInt(bound);
    }
}
