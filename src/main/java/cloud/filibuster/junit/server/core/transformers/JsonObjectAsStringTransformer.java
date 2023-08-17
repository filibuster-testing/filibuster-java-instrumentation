package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterTransformerException;
import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import static cloud.filibuster.junit.server.core.FilibusterCoreTransformerExtension.getTransformerInstance;
import static cloud.filibuster.junit.server.core.transformers.selector.GatewayTransformer.getTransformerClassNameFromReferenceValue;

public final class JsonObjectAsStringTransformer implements Transformer<String, List<SimpleImmutableEntry<String, String>>> {
    private boolean hasNext = true;
    private String result;
    private Accumulator<String, List<SimpleImmutableEntry<String, String>>> accumulator;
    private Transformer<?, ?> lastCtxEntryTransformationResult;
    private static final Gson gson = new Gson();

    @Override
    @CanIgnoreReturnValue
    public JsonObjectAsStringTransformer transform(String payload, Accumulator<String, List<SimpleImmutableEntry<String, String>>> accumulator) {
        List<SimpleImmutableEntry<String, String>> ctx = accumulator.getContext();

        // Convert payload to JSON object and flatten it
        JSONObject payloadJo = new JSONObject(payload);
        payloadJo = JsonUtils.flatten(payloadJo);

        // Get the last context entry and the value to transform
        // The last context entry is the one that has not been transformed yet
        // Format of entries in the context: <key, accumulator>
        SimpleImmutableEntry<String, String> lastCtxEntry = ctx.get(ctx.size() - 1);
        Object valueToTransform = payloadJo.get(lastCtxEntry.getKey());

        // Get the transformer and accumulator for the value to transform
        String transformerClassName = getTransformerClassNameFromReferenceValue(valueToTransform);
        Transformer<?, ?> lastCtxEntryTransformer = getTransformerInstance(transformerClassName);
        Accumulator<?, ?> lastCtxEntryAccumulator = gson.fromJson(lastCtxEntry.getValue(), lastCtxEntryTransformer.getAccumulatorType());

        try {  // Invoke the transform method of the transformer
            Method transformMethod = lastCtxEntryTransformer.getClass().getMethod("transform", (Class<?>) lastCtxEntryTransformer.getPayloadType(), Accumulator.class);

            lastCtxEntryTransformationResult =
                    (Transformer<?, ?>) transformMethod.invoke(
                            lastCtxEntryTransformer,
                            lastCtxEntryAccumulator.getReferenceValue(),
                            lastCtxEntryAccumulator
                    );
            payloadJo.put(lastCtxEntry.getKey(), lastCtxEntryTransformationResult.getResult().toString());

            // If the last transformation result has a next accumulator, update the context
            if (lastCtxEntryTransformationResult.hasNext()) {
                lastCtxEntryAccumulator = lastCtxEntryTransformationResult.getNextAccumulator();
                ctx.set(ctx.size() - 1, new SimpleImmutableEntry<>(lastCtxEntry.getKey(), gson.toJson(lastCtxEntryAccumulator)));
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new FilibusterTransformerException("[JsonObjectAsStringTransformer]: An exception occurred while invoking transform method of transformer " + lastCtxEntryTransformer.getClass().getName(), e);
        }

        // If the context has all the keys in the payload, set hasNext to false
        if (ctx.size() == payloadJo.keySet().size() && !lastCtxEntryTransformationResult.hasNext()) {
            this.hasNext = false;
        }

        // Update the result and the accumulator
        this.result = JsonUtils.unflatten(payloadJo).toString();  // Unflatten the JSON object and convert it to string
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
        Type stringType = TypeToken.get(String.class).getType();
        Type simpleEntryType = TypeToken.getParameterized(SimpleImmutableEntry.class, String.class, String.class).getType();
        Type listType = TypeToken.getParameterized(List.class, simpleEntryType).getType();

        return TypeToken.getParameterized(
                Accumulator.class,
                stringType,
                listType).getType();
    }

    @Override
    public Accumulator<String, List<SimpleImmutableEntry<String, String>>> getInitialAccumulator(String referenceValue) {
        // Prepare initial context
        List<SimpleImmutableEntry<String, String>> ctx = new ArrayList<>();
        JSONObject referenceJo = new JSONObject(referenceValue);
        referenceJo = JsonUtils.flatten(referenceJo);
        // If the reference value is an empty JSON object, do not add anything to the context
        if (referenceJo.keySet().size() > 0) {
            String firstKey = referenceJo.keySet().iterator().next();
            Object firstValue = referenceJo.get(firstKey);

            Accumulator<?, ?> initialAccumulator = getInitialAccumulatorFromValue(firstValue);

            SimpleImmutableEntry<String, String> entry = new SimpleImmutableEntry<>(firstKey, gson.toJson(initialAccumulator));
            ctx.add(entry);
        } else {
            this.hasNext = false;
        }

        Accumulator<String, List<SimpleImmutableEntry<String, String>>> accumulator = new Accumulator<>();
        accumulator.setContext(ctx);
        accumulator.setReferenceValue(referenceValue);
        this.result = referenceValue;
        return accumulator;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Accumulator<?, ?> getInitialAccumulatorFromValue(Object value) {
        String transformerClassName = getTransformerClassNameFromReferenceValue(value);
        Transformer transformer = getTransformerInstance(transformerClassName);
        Accumulator accumulator = transformer.getInitialAccumulator();
        accumulator.setReferenceValue(value);
        return accumulator;
    }

    @Override
    public Accumulator<String, List<SimpleImmutableEntry<String, String>>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            // Check if the last transformation result doesn't have a next transformation
            if (!lastCtxEntryTransformationResult.hasNext()) {
                // Get the context and create the reference JSON object from the reference value
                List<SimpleImmutableEntry<String, String>> ctx = accumulator.getContext();
                JSONObject referenceJo = new JSONObject(accumulator.getReferenceValue());

                // Flatten the reference JSON object
                referenceJo = JsonUtils.flatten(referenceJo);

                // If the context has all the keys in the reference value, set hasNext to false
                if (ctx.size() == referenceJo.keySet().size()) {
                    this.hasNext = false;
                } else {
                    try {
                        // Get the next key and value from the reference JSON object
                        String nextKey = new ArrayList<>(referenceJo.keySet()).get(ctx.size());
                        Object nextValue = referenceJo.get(nextKey);

                        // Get the initial accumulator for the new value
                        Accumulator<?, ?> initialAccumulator = getInitialAccumulatorFromValue(nextValue);
                        SimpleImmutableEntry<String, String> entry = new SimpleImmutableEntry<>(nextKey, gson.toJson(initialAccumulator));

                        // Update the context
                        ctx.add(entry);
                        accumulator.setContext(ctx);
                    } catch (RuntimeException e) {
                        throw new FilibusterTransformerException("[JsonObjectAsStringTransformer]: An exception occurred while getting next accumulator", e);
                    }
                }
            }
            return accumulator;
        }
    }
}
