package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcMultipleFaultsInjectedException} is thrown when assertions in assertTestBlock fail due to multiple faults being injected.
 * Please use assertOnFault to update assertions so that they hold under fault.
 */
public class FilibusterGrpcMultipleFaultsInjectedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcMultipleFaultsInjectedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcMultipleFaultsInjectedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertTestBlock failed due to multiple faults being injected.\nPlease use assertOnFault to update assertions so that they hold under fault.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Specify fault results in different assertions by combined fault specification:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(cloud.filibuster.junit.statem.CompositeFaultSpecification,java.lang.Runnable)")
                )
        );
    }
}
