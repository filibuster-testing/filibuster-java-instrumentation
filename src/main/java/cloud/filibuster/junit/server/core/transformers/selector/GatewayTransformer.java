package cloud.filibuster.junit.server.core.transformers.selector;


import cloud.filibuster.exceptions.filibuster.FilibusterTransformerException;
import cloud.filibuster.junit.server.core.transformers.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class GatewayTransformer implements Transformer<Object, Object> {

    // Entries have the format <payloadClass, Function<payloadValue, transformerClass>>
    // For example, <String.class.getName(), ("This is a string") -> StringTransformer.class>
    // <String.class.getName(), ("{/"key/": /"value/"}") -> JsonObjectTransformer.class>
    private static final Map<String, Function<String, Class<? extends Transformer<?, ?>>>> payloadClassToTransformerSelectorMethod = new HashMap<>();

    static {
        payloadClassToTransformerSelectorMethod.put(String.class.getName(), getSelectorMethod(new StringSelector()));
        payloadClassToTransformerSelectorMethod.put(Object.class.getName(), getSelectorMethod(new ObjectSelector()));
        payloadClassToTransformerSelectorMethod.put(byte[].class.getName(), getSelectorMethod(new ByteArraySelector()));
    }

    private static <T extends Selector> Function<String, Class<? extends Transformer<?, ?>>> getSelectorMethod(T selector) {
        return selector::select;
    }

    public static String getTransformerClassNameFromReferenceValue(String payloadClass, String referenceValue) {
        try {
            return payloadClassToTransformerSelectorMethod.get(payloadClass).apply(referenceValue).getName();
        } catch (NullPointerException e) {
            throw new FilibusterTransformerException("No transformer found for payload class " + payloadClass + " and reference value " + referenceValue, e);
        }
    }
}
