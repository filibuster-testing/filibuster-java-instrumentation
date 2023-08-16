package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.BooleanAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.JsonObjectAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.StringTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import org.json.JSONObject;

class UnknownTypeSelector extends Selector {

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
}
