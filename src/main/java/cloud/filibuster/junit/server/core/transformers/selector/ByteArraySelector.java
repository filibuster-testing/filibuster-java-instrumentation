package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.BitInByteArrTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;


class ByteArraySelector extends Selector {

    @Override
    <T> Class<? extends Transformer<byte[], ?>> select(T payloadValue) {
        return BitInByteArrTransformer.class;
    }
}
