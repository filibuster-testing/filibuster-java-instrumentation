package cloud.filibuster.integration.instrumentation;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexType;
import cloud.filibuster.instrumentation.helpers.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class FilibusterBaseTest {
    @BeforeAll
    public static void enableInstrumentation() {
        Property.setInstrumentationEnabledProperty(true);
    }

    @AfterAll
    public static void disableInstrumentation() {
        Property.setInstrumentationEnabledProperty(false);
    }

    public DistributedExecutionIndex createNewDistributedExecutionIndex() {
        return DistributedExecutionIndexType.getImplType().createImpl();
    }
}
