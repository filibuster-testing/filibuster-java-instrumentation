package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;

import java.lang.reflect.Type;

public final class JsonObjectAsStringTransformer implements Transformer<String, String> {
    // TODO
    private boolean hasNext = false;
    private String result;
    private Accumulator<String, String> accumulator;

    @Override
    @CanIgnoreReturnValue
    public JsonObjectAsStringTransformer transform(String payload, Accumulator<String, String> accumulator) {
        // TODO
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
                JSONObject.class,
                String.class).getType();
    }

    @Override
    public Accumulator<String, String> getInitialAccumulator() {
        Accumulator<String, String> accumulator = new Accumulator<>();
        accumulator.setContext("");
        return accumulator;
    }

    @Override
    public Accumulator<String, String> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            accumulator.setContext(accumulator.getContext()); // TODO
            return accumulator;
        }
    }
}
