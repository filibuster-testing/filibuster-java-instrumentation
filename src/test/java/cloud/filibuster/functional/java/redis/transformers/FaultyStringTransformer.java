package cloud.filibuster.functional.java.redis.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public final class FaultyStringTransformer implements Transformer<String, Integer> {
    private boolean hasNext = true;
    private String payload;
    private Accumulator<String, Integer> accumulator;

    @Override
    @CanIgnoreReturnValue
    public FaultyStringTransformer transform(String payload, Accumulator<String, Integer> accumulator) {
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
    public Type getPayloadType() {
        return String.class;
    }

    @Override
    public String getResult() {
        if (this.payload == null) {
            throw new TransformerNullResultException("Result is null. getResult() was probably called before transform()!");
        }
        return this.payload;
    }

    @Override
    public Accumulator<String, Integer> getInitialAccumulator() {
        Accumulator<String, Integer> accumulator = new Accumulator<>();
        accumulator.setContext(0);
        return accumulator;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                String.class,
                Integer.class).getType();
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

}
