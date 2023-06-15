package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;

import javax.annotation.Nullable;

public final class FaultyByzantineStringTransformer implements ByzantineTransformer<String, Integer> {
    private boolean hasNext = true;
    private String payload;
    private Accumulator<String, Integer> accumulator;

    @Override
    public FaultyByzantineStringTransformer transform(String payload, @Nullable Accumulator<String, Integer> accumulator) {
        if (accumulator == null) {
            accumulator = getInitialAccumulator();
        }

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
        return 'X';  // Fault: always return 'X'
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
            accumulator.setContext(accumulator.getContext()); // Fault: Do not increment counter
            return accumulator;
        }
    }

    @Override
    public Class<String> getPayloadType() {
        return String.class;
    }

    @Override
    public Class<Integer> getCounterType() {
        return Integer.class;
    }
}
