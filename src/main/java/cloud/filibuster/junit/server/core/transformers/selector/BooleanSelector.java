package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.BooleanAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;


class BooleanSelector extends Selector {

    @Override
    <T> Class<? extends Transformer<?, ?>> select(T payloadValue) {
        return BooleanAsStringTransformer.class;
    }
}
