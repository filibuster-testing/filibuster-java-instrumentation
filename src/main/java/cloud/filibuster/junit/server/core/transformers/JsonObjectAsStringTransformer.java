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

import static cloud.filibuster.junit.server.core.FilibusterCoreTransformerExtension.getTransformerInstance;
import static cloud.filibuster.junit.server.core.transformers.selector.GatewayTransformer.getTransformerClassNameFromReferenceValue;

public final class JsonObjectAsStringTransformer implements Transformer<String, ArrayList<SimpleImmutableEntry<String, String>>> {
    private boolean hasNext = true;
    private String result;
    private Accumulator<String, ArrayList<SimpleImmutableEntry<String, String>>> accumulator;
    private Transformer<?, ?> lastTransformationResult;
    private static final Gson gson = new Gson();

    @Override
    @CanIgnoreReturnValue
    public JsonObjectAsStringTransformer transform(String payload, Accumulator<String, ArrayList<SimpleImmutableEntry<String, String>>> accumulator) {
        ArrayList<SimpleImmutableEntry<String, String>> ctx = accumulator.getContext();

        JSONObject payloadJO = new JSONObject(payload);

        SimpleImmutableEntry<String, String> lastCtxEntry = ctx.get(ctx.size() - 1);
        String valueToTransform = payloadJO.getString(lastCtxEntry.getKey());

        String transformerClassName = getTransformerClassNameFromReferenceValue(String.class.getName(), valueToTransform);
        Transformer<?, ?> lastTransformer = getTransformerInstance(transformerClassName);
        Accumulator<?, ?> lastAccumulator = gson.fromJson(lastCtxEntry.getValue(), lastTransformer.getAccumulatorType());

        try {
            Method transformMethod = lastTransformer.getClass().getMethod("transform", (Class<?>) lastTransformer.getPayloadType(), Accumulator.class);

            lastTransformationResult =
                    (Transformer<?, ?>) transformMethod.invoke(
                            lastTransformer,
                            payloadJO.getString(lastCtxEntry.getKey()),
                            lastAccumulator
                    );
            payloadJO.put(lastCtxEntry.getKey(), lastTransformationResult.getResult().toString());
            if (lastTransformationResult.hasNext()) {
                lastAccumulator = lastTransformationResult.getNextAccumulator();
                ctx.set(ctx.size() - 1, new SimpleImmutableEntry<>(lastCtxEntry.getKey(), gson.toJson(lastAccumulator)));
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new FilibusterTransformerException("[JsonObjectAsStringTransformer]: An exception occurred while invoking transform method of transformer " + lastTransformer.getClass().getName(), e);
        }

        // If the context has all the keys in the payload, set hasNext to false
        if (ctx.size() == payloadJO.keySet().size() && !lastTransformationResult.hasNext()) {
            this.hasNext = false;
        }

        this.result = payloadJO.toString();
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
        Type listType = TypeToken.getParameterized(ArrayList.class, simpleEntryType).getType();

        return TypeToken.getParameterized(
                Accumulator.class,
                stringType,
                listType).getType();
    }

    @Override
    public Accumulator<String, ArrayList<SimpleImmutableEntry<String, String>>> getInitialAccumulator(String referenceValue) {
        // Prepare initial context
        ArrayList<SimpleImmutableEntry<String, String>> ctx = new ArrayList<>();
        JSONObject referenceJO = new JSONObject(referenceValue);
        // If the reference value is an empty JSON object, do not add anything to the context
        if (referenceJO.keySet().size() > 0) {
            String firstKey = referenceJO.keySet().iterator().next();
            String firstValue = referenceJO.getString(firstKey);
            String transformerClassName = getTransformerClassNameFromReferenceValue(String.class.getName(), firstValue);

            Transformer<?, ?> transformerObject = getTransformerInstance(transformerClassName);
            Accumulator<?, ?> initialAccumulator = transformerObject.getInitialAccumulator(gson.fromJson(firstValue, transformerObject.getPayloadType()));


            SimpleImmutableEntry<String, String> entry = new SimpleImmutableEntry<>(firstKey, gson.toJson(initialAccumulator));
            ctx.add(entry);
        } else {
            this.hasNext = false;
        }

        Accumulator<String, ArrayList<SimpleImmutableEntry<String, String>>> accumulator = new Accumulator<>();
        accumulator.setContext(ctx);
        accumulator.setReferenceValue(referenceValue);
        this.result = referenceValue;
        return accumulator;
    }

    @Override
    public Accumulator<String, ArrayList<SimpleImmutableEntry<String, String>>> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            if (!lastTransformationResult.hasNext()) {
                ArrayList<SimpleImmutableEntry<String, String>> ctx = accumulator.getContext();
                JSONObject referenceJO = new JSONObject(accumulator.getReferenceValue());
                if (ctx.size() == referenceJO.keySet().size()) {
                    this.hasNext = false;
                } else {
                    String nextKey = new ArrayList<>(referenceJO.keySet()).get(ctx.size());
                    String nextValue = referenceJO.getString(nextKey);
                    String transformerClassName = getTransformerClassNameFromReferenceValue(String.class.getName(), nextValue);

                    Transformer<?, ?> transformerObject = getTransformerInstance(transformerClassName);
                    Accumulator<?, ?> initialAccumulator = transformerObject.getInitialAccumulator(gson.fromJson(nextValue, transformerObject.getPayloadType()));

                    SimpleImmutableEntry<String, String> entry = new SimpleImmutableEntry<>(nextKey, gson.toJson(initialAccumulator));
                    ctx.add(entry);
                    accumulator.setContext(ctx);
                }
            }
            return accumulator;
        }
    }
}
