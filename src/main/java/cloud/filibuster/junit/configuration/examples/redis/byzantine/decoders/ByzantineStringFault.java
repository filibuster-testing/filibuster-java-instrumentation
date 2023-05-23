package cloud.filibuster.junit.configuration.examples.redis.byzantine.decoders;

import javax.annotation.Nullable;

public final class ByzantineStringFault extends ByzantineFaultCaster<String> {
    @Override
    @Nullable
    public String cast(Object byzantineFaultValue) {
        return byzantineFaultValue != null ? byzantineFaultValue.toString() : null;
    }

    @Override
    public ByzantineFaultType getFaultType() {
        return ByzantineFaultType.STRING;
    }
}
