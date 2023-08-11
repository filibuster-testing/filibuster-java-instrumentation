package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public final class BooleanAsStringTransformer implements Transformer<String, String> {
    // TODO
    private boolean hasNext = true;
    private String result;
    private Accumulator<String, String> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BooleanAsStringTransformer transform(String payload, Accumulator<String, String> accumulator) {
        boolean boolValue = Boolean.parseBoolean(payload);
        boolValue = !boolValue;

        this.result = String.valueOf(boolValue);
        this.accumulator = accumulator;

        this.hasNext = false;
        return this;
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
        if (this.result == null) {
            throw new TransformerNullResultException("Result is null. getResult() was probably called before transform()!");
        }
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                String.class,
                String.class).getType();
    }

    @Override
    public Accumulator<String, String> getInitialAccumulator() {
        return new Accumulator<>();
    }

    @Override
    public Accumulator<String, String> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            return accumulator;
        }
    }
}
