package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import javax.annotation.Nonnull;

public interface ByzantineTransformer<PAYLOAD, ACCUMULATOR>  {
    PAYLOAD transform(PAYLOAD payload, @Nonnull ACCUMULATOR accumulator);
    ACCUMULATOR getNewAccumulator();
    boolean hasNext();
}