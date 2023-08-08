package cloud.filibuster.junit.configuration.examples.db.byzantine.types;

import org.json.JSONObject;

import javax.annotation.Nullable;

public final class ByzantineStringFaultType extends ByzantineFaultType<String> {
    @Override
    @Nullable
    public String cast(Object byzantineFaultValue) {
        if (byzantineFaultValue == null || byzantineFaultValue == JSONObject.NULL) {
            return null;
        }
        return byzantineFaultValue.toString();
    }

    @Override
    public ByzantineFault getFaultType() {
        return ByzantineFault.STRING;
    }
}
