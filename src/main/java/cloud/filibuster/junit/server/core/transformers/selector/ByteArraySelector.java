package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.BitInByteArrTransformer;
import cloud.filibuster.junit.server.core.transformers.BooleanAsByteArrTransformer;
import cloud.filibuster.junit.server.core.transformers.JsonObjectAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.StringAsByteArrTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import org.json.JSONObject;

import java.nio.charset.Charset;


class ByteArraySelector extends Selector {
    @Override
    <T> Class<? extends Transformer<?, ?>> select(T payloadValue) {

        if (isApplicable(byte[].class, payloadValue, ByteArraySelector::getJsonObj)) {
            return JsonObjectAsStringTransformer.class;
        }

        if (isApplicable(byte[].class, payloadValue, ByteArraySelector::isBoolean)) {
            return BooleanAsByteArrTransformer.class;
        }

        if (isApplicable(byte[].class, payloadValue, ByteArraySelector::getString)) {
            return StringAsByteArrTransformer.class;
        }

        return BitInByteArrTransformer.class;
    }

    static JSONObject getJsonObj(byte[] payloadValue) {
        String payloadValueStr = new String(payloadValue, Charset.defaultCharset());
        return new JSONObject(payloadValueStr);
    }

    static boolean isBoolean(byte[] payloadValue) {
        String payloadValueStr = new String(payloadValue, Charset.defaultCharset());
        return Selector.isBoolean(payloadValueStr);
    }

    static String getString(byte[] payloadValue) {
        return new String(payloadValue, Charset.defaultCharset());
    }
}
