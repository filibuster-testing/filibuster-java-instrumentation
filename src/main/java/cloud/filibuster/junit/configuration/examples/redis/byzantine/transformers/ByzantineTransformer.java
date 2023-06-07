package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

public interface ByzantineTransformer<T>  {
    T transform(T payload, T accumulator, int idx);
}