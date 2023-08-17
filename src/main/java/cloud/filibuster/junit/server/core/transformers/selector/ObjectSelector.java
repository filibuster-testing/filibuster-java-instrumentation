package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.NullTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;


class ObjectSelector extends Selector {

    @Override
    <T> Class<? extends Transformer<Object, ?>> select(T payloadValue) {
        return NullTransformer.class;
    }
}
