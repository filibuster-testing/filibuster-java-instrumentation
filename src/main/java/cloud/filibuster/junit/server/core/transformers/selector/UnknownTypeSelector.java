package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.BooleanAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.JsonObjectAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.StringTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import org.json.JSONObject;

import java.util.regex.Pattern;

class UnknownTypeSelector extends Selector {
    private static final Pattern boolPattern = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);

    @Override
    <T> Class<? extends Transformer<String, ?>> select(T payloadValue) {

        if (isApplicable(String.class, payloadValue, JSONObject::new)) {
            return JsonObjectAsStringTransformer.class;
        }

        if (isApplicable(String.class, payloadValue, UnknownTypeSelector::isBoolean)) {
            return BooleanAsStringTransformer.class;
        }

        return StringTransformer.class;
    }

    private static boolean isBoolean(String value) {
        if (boolPattern.matcher(value).matches()) {
            return true;
        } else {
            throw new IllegalArgumentException("Value is not a boolean: " + value);
        }
    }
}