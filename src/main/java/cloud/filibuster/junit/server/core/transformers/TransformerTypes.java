package cloud.filibuster.junit.server.core.transformers;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public enum TransformerTypes {

    STRING(StringTransformer.class, String.class),
    BYTE_ARRAY(BitInByteArrTransformer.class, byte[].class),
    OBJECT(NullTransformer.class, Object.class),
    JSON_OBJECT(JsonObjectTransformer.class, JSONObject.class);

    private final Class<? extends Transformer<?, ?>> transformerClass;
    private final Class<?> payloadClass;
    private static final Map<String, String> transformerClassNames = new HashMap<>();

    static {
        for (TransformerTypes e : values()) {
            transformerClassNames.put(e.payloadClass.getName(), e.transformerClass.getName());
        }
    }

    TransformerTypes(Class<? extends Transformer<?, ?>> transformerClass, Class<?> payloadClass) {
        this.transformerClass = transformerClass;
        this.payloadClass = payloadClass;
    }

    public static String getTransformerClassNameByPayloadClass(String payloadClass) {
        return transformerClassNames.get(payloadClass);
    }

}
