package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.JsonObjectAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.StringTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import org.json.JSONObject;

class StringSelector extends Selector {

    @Override
    Class<? extends Transformer<String, ?>> select(String payloadValue) {

        if (isApplicable(String.class, payloadValue, JSONObject::new)) {
            return JsonObjectAsStringTransformer.class;
        }

        return StringTransformer.class;
    }
}
