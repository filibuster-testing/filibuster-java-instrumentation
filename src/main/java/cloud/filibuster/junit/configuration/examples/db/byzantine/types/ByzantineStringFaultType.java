package cloud.filibuster.junit.configuration.examples.db.byzantine.types;

import javax.annotation.Nullable;

public final class ByzantineStringFaultType extends ByzantineFaultType<String> {
    @Override
    @Nullable
    public String cast(Object byzantineFaultValue) {
        return byzantineFaultValue != null ? byzantineFaultValue.toString() : null;
    }

    @Override
    public ByzantineFault getFaultType() {
        return ByzantineFault.STRING;
    }
}
