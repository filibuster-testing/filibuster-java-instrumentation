package cloud.filibuster.junit.server.core.transformers;


public abstract class GatewayTransformer implements Transformer<Object, Object> {
    public static String getTransformerClassNameFromReferenceValueType(String referenceValueType) {
        return TransformerTypes.getTransformerClassNameByPayloadClass(referenceValueType);
    }
}
