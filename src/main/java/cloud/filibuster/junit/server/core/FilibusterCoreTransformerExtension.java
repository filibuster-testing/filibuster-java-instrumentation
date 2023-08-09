package cloud.filibuster.junit.server.core;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import com.google.gson.Gson;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.logging.Logger;

public final class FilibusterCoreTransformerExtension {
    private static final Logger logger = Logger.getLogger(FilibusterCoreTransformerExtension.class.getName());


    public static <PAYLOAD, CONTEXT> Transformer<PAYLOAD, CONTEXT> getTransformerResult(@Nonnull JSONObject transformer) {
        try {
            if (transformer.has("accumulator") && transformer.has("transformerClassName")) {

                // Get transformer object.
                Transformer<?, ?> transformerObject = getTransformerInstance(transformer.getString("transformerClassName"));

                // Get transform method of transformer object.
                Method transformMethod = transformerObject.getClass().getMethod("transform", (Class<?>) transformerObject.getPayloadType(), Accumulator.class);

                // Get the accumulator.
                Type accumulatorType = transformerObject.getAccumulatorType();
                Accumulator<?, ?> accumulator = new Gson().fromJson(String.valueOf(transformer.get("accumulator")), accumulatorType);

                // Get the reference value.
                Object referenceValue = ((Class<?>) transformerObject.getPayloadType()).cast(accumulator.getReferenceValue());

                // Invoke transform method.
                @SuppressWarnings("unchecked")
                Transformer<PAYLOAD, CONTEXT> transformationResult =
                        (Transformer<PAYLOAD, CONTEXT>) transformMethod.invoke(
                                transformerObject,
                                referenceValue,
                                accumulator
                        );
                // Return the transformation result.
                return transformationResult;
            } else {
                logger.warning("[FILIBUSTER-CORE]: getTransformerResult, transformer is missing required keys, either 'accumulator', 'referenceValue' or 'transformerClassName'.");
                throw new FilibusterFaultInjectionException("[FILIBUSTER-CORE]: getTransformerResult, transformer is missing required keys, either 'accumulator', 'referenceValue' or 'transformerClassName': " + transformer);
            }
        } catch (Exception e) {
            logger.warning("[FILIBUSTER-CORE]: getTransformerResult, an exception occurred: " + e);
            throw new FilibusterFaultInjectionException("[FILIBUSTER-CORE]: getTransformerResult, an exception occurred: " + e);
        }
    }


    public static void setNextAccumulator(JSONObject transformer, Accumulator<?, ?> accumulator) {
        if (transformer.has("transformerClassName")) {
            Transformer<?, ?> transformerObject = getTransformerInstance(transformer.getString("transformerClassName"));
            Type accumulatorType = transformerObject.getAccumulatorType();
            transformer.put("accumulator", new Gson().toJson(accumulator, accumulatorType));
        }
    }

    public static void generateAndSetTransformerValue(JSONObject transformer) {
        Transformer<?, ?> transformerResult = getTransformerResult(transformer);
        Object result = transformerResult.getResult();
        setTransformerValue(transformer, result);
    }

    public static void setTransformerValue(JSONObject transformer, Object value) {
        if (value == null) {
            transformer.put("value", JSONObject.NULL);
        } else {
            transformer.put("value", value);
        }
    }


    public static Accumulator<?, ?> getInitialAccumulator(JSONObject transformer, String referenceValue) {
        if (transformer.has("transformerClassName")) {
            String transformerClassName = transformer.getString("transformerClassName");
            Transformer<?, ?> transformerObject = getTransformerInstance(transformerClassName);
            Accumulator<?, ?> initialAccumulator = transformerObject.getInitialAccumulator();
            initialAccumulator.setReferenceValue(new Gson().fromJson(referenceValue, transformerObject.getPayloadType()));
            return initialAccumulator;
        } else {
            throw new FilibusterFaultInjectionException("[FILIBUSTER-CORE]: getInitialAccumulator, transformerClassName not found in transformer: " + transformer.toString(4));
        }
    }


    private static Transformer<?, ?> getTransformerInstance(String transformerClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Transformer<?, ?>> transformerClass = (Class<? extends Transformer<?, ?>>) Class.forName(transformerClassName);
            // Get constructor of transformer class.
            Constructor<?> ctr = transformerClass.getConstructor();
            // Create transformer object.
            return (Transformer<?, ?>) ctr.newInstance();
        } catch (Exception e) {
            logger.warning("[FILIBUSTER-CORE]: getTransformerInstance, an exception occurred in getTransformerInstance: " + e);
            throw new FilibusterFaultInjectionException("[FILIBUSTER-CORE]: getTransformerInstance, an exception occurred in getTransformerInstance: " + e);
        }
    }
}
