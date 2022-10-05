package cloud.filibuster.instrumentation;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexType;
import cloud.filibuster.instrumentation.helpers.Property;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.setHashProperty;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.setStackTraceIncludeProperty;

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

    public static void setPrettyDistributedExecutionIndexes() {
        setHashProperty(false);
        setStackTraceIncludeProperty(false);
    }

    public static void unsetPrettyDistributedExecutionIndexes() {
        setHashProperty(true);
        setStackTraceIncludeProperty(true);
    }
}
