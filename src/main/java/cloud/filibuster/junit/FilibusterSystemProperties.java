package cloud.filibuster.junit;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;

public class FilibusterSystemProperties {
    private FilibusterSystemProperties() {

    }

    public static void setSystemPropertiesForFilibusterInstrumentation(FilibusterConfiguration filibusterConfiguration) {
        if (filibusterConfiguration.getDataNondeterminism()) {
            DistributedExecutionIndexV1.Properties.Asynchronous.setAsynchronousInclude(false);
        }

        System.setProperty("kotlinx.coroutines.debug", "on");
        System.setProperty("kotlinx.coroutines.stacktrace.recovery", "true");
    }

    public static void unsetSystemPropertiesForFilibusterInstrumentation() {
        DistributedExecutionIndexV1.Properties.Asynchronous.setAsynchronousInclude(true);

        System.setProperty("kotlinx.coroutines.debug", "off");
        System.setProperty("kotlinx.coroutines.stacktrace.recovery", "false");
    }
}
