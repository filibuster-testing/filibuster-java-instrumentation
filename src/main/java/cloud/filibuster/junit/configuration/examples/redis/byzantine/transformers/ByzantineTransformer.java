package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

public interface ByzantineTransformer<T, U>  {
    T transform(T payload, U accumulator);
}