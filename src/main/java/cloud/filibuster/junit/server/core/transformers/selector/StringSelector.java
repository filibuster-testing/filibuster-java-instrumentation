package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.BooleanAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.JsonObjectAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.StringTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import org.json.JSONObject;

import java.util.regex.Pattern;

class StringSelector extends Selector {

    @Override
    Class<? extends Transformer<String, ?>> select(String payloadValue) {

        if (isApplicable(String.class, payloadValue, JSONObject::new)) {
            return JsonObjectAsStringTransformer.class;
        }

        if (isApplicable(String.class, payloadValue, this::isBoolean)) {
            return BooleanAsStringTransformer.class;
        }

        return StringTransformer.class;
    }

    private boolean isBoolean(String value) {
        if (Pattern.compile("true|false", Pattern.CASE_INSENSITIVE).matcher(value).matches()) {
            return true;
        } else {
            throw new IllegalArgumentException("Value is not a boolean: " + value);
        }
    }
}
