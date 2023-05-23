package cloud.filibuster.junit.configuration.examples.redis.byzantine.decoders;

public final class ByzantineStringFault implements ByzantineDecoder<String> {
    @Override
    public String decode(Object byzantineFaultValue) {
        return byzantineFaultValue.toString();
    }
}
