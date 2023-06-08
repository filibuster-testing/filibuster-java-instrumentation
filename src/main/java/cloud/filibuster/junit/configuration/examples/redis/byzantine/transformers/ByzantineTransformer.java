package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

public interface ByzantineTransformer<PAYLOAD, ACCUMULATOR>  {
    PAYLOAD transform(PAYLOAD payload, ACCUMULATOR accumulator);
    boolean hasNext();
}