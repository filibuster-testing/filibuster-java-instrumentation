package cloud.filibuster.junit.server.core.transformers;

import javax.annotation.Nullable;

public interface ByzantineTransformer<PAYLOAD, CONTEXT> {
    ByzantineTransformer<PAYLOAD, CONTEXT> transform(PAYLOAD payload, @Nullable Accumulator<PAYLOAD, CONTEXT> accumulator);

    boolean hasNext();

    Class<PAYLOAD> getPayloadType();
    Class<CONTEXT> getCounterType();
    PAYLOAD getResult();
    Accumulator<PAYLOAD, CONTEXT> getAccumulator();
    Accumulator<PAYLOAD, CONTEXT> getInitialAccumulator();
    Accumulator<PAYLOAD, CONTEXT> getNextAccumulator();
}