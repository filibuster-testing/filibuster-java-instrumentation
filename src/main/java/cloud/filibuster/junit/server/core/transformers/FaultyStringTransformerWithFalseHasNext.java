package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Random;

public final class FaultyStringTransformerWithFalseHasNext implements Transformer<String, Integer> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results

    private boolean hasNext = false; // Fault: initial hasNext value is false
    private String payload;
    private Accumulator<String, Integer> accumulator;

    @Override
    @CanIgnoreReturnValue
    public FaultyStringTransformerWithFalseHasNext transform(String payload, Accumulator<String, Integer> accumulator) {
        int idx = accumulator.getContext();

        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, generateRandomChar());

        if (idx == payload.length() - 1) {
            this.hasNext = false;
        }

        this.payload = newString.toString();
        this.accumulator = accumulator;

        return this;
    }

    private static char generateRandomChar() {
        // ASCII printable characters range from 33 to 126. Upper bound in nextInt is exclusive, hence 127.
        return (char) (rand.nextInt(127) + 33);
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public String getResult() {
        if (this.payload == null) {
            throw new FilibusterFaultInjectionException("getResult() called before transform()!");
        }
        return this.payload;
    }

    @Override
    public Accumulator<String, Integer> getAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        }
        return this.accumulator;
    }

    @Override
    public Accumulator<String, Integer> getInitialAccumulator() {
        Accumulator<String, Integer> accumulator = new Accumulator<>();
        accumulator.setContext(0);
        return accumulator;
    }

    @Override
    public Accumulator<String, Integer> getNextAccumulator() {
        if (accumulator == null) {
            return getInitialAccumulator();
        } else {
            accumulator.setContext(accumulator.getContext() + 1);
            return accumulator;
        }
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public Class<Integer> getContextType() {
        return Integer.class;
    }

}
