package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcAmbiguousFailureHandlingException} indicates that the compositional verification failed due to ambiguous failure handling.
 * Each fault introduced has different impact.
 * To fix this exception, write an assertOnFault(...) for this fault combination with appropriate assertions.
 */
public class FilibusterGrpcAmbiguousFailureHandlingException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousFailureHandlingException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousFailureHandlingException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Compositional verification failed due to ambiguous failure handling: each fault introduced has different impact.\nPlease write an assertOnFault(...) for this fault combination with appropriate assertions.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Write an assertion for the current fault combination:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(cloud.filibuster.junit.statem.CompositeFaultSpecification,java.lang.Runnable)"
                        )
                )
        );
    }
}
