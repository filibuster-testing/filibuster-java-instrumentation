package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;

import java.lang.reflect.Type;

public final class JsonObjectTransformer implements Transformer<JSONObject, String> {
    // TODO
    private boolean hasNext = true;
    private JSONObject result;
    private Accumulator<JSONObject, String> accumulator;

    @Override
    @CanIgnoreReturnValue
    public JsonObjectTransformer transform(JSONObject payload, Accumulator<JSONObject, String> accumulator) {
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
    public JSONObject getResult() {
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
    public Accumulator<JSONObject, String> getInitialAccumulator() {
        Accumulator<JSONObject, String> accumulator = new Accumulator<>();
        accumulator.setContext("");
        return accumulator;
    }

    @Override
    public Accumulator<JSONObject, String> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            accumulator.setContext(accumulator.getContext()); // TODO
            return accumulator;
        }
    }
}
