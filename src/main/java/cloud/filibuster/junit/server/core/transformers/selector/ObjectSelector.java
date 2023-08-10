package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.NullTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;


class ObjectSelector extends Selector {

    @Override
    Class<? extends Transformer<Object, ?>> select(String payloadValue) {
        return NullTransformer.class;
    }
}
