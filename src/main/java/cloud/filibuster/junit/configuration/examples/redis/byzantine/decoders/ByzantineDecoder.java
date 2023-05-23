package cloud.filibuster.junit.configuration.examples.redis.byzantine.decoders;

public interface ByzantineDecoder<T> {
    T decode(Object byzantineFaultValue);
    // TODO create to string function
    // TODO lookup how to go back from String to object
}
