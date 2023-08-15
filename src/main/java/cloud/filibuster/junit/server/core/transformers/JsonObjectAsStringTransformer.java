package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class JsonObjectAsStringTransformer implements Transformer<String, List<String>> {
    private boolean hasNext = true;
    private String result;
    private Accumulator<String, List<String>> accumulator;

    @Override
    @CanIgnoreReturnValue
    public JsonObjectAsStringTransformer transform(String payload, Accumulator<String, List<String>> accumulator) {
        List<String> ctx = accumulator.getContext();

        JSONObject payloadJsonObject = new JSONObject(payload);

        // If the context is empty, add the first key in the payload to the context
        // That is the case in the first iteration
        if (ctx.size() == 0 && payloadJsonObject.keySet().size() > 0) {
            ctx.add(payloadJsonObject.keySet().iterator().next());
            accumulator.setContext(ctx);
        }

        // If the context has all the keys in the payload, set hasNext to false
        if (ctx.size() == payloadJsonObject.keySet().size()) {
            this.hasNext = false;
        }

        payloadJsonObject.remove(ctx.get(ctx.size() - 1));  // Remove the last key in the ctx from the payload

        this.result = payloadJsonObject.toString();
        this.accumulator = accumulator;

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
        Type listType = TypeToken.getParameterized(List.class, String.class).getType();

        return TypeToken.getParameterized(
                Accumulator.class,
                String.class,
                listType).getType();
    }

    @Override
    public Accumulator<String, List<String>> getInitialAccumulator(String referenceValue) {
        // Prepare initial context
        List<String> ctx = new ArrayList<>();
        JSONObject referenceJsonObject = new JSONObject(referenceValue);
        if (referenceJsonObject.keySet().size() > 0) {  // If the reference value is an empty JSON object, do not add anything to the context
            ctx.add(referenceJsonObject.keySet().iterator().next());
        } else {
            this.hasNext = false;
        }

        Accumulator<String, List<String>> accumulator = new Accumulator<>();
        accumulator.setContext(ctx);
        accumulator.setReferenceValue(referenceValue);
        this.result = referenceValue;
        return accumulator;
    }

    @Override
    public Accumulator<String, List<String>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            List<String> ctx = accumulator.getContext();
            JSONObject referenceJsonObject = new JSONObject(accumulator.getReferenceValue());

            for (String key : referenceJsonObject.keySet()) {
                if (!ctx.contains(key)) {
                    ctx.add(key);
                    break;
                }
            }
            accumulator.setContext(ctx);
            return accumulator;
        }
    }
}
