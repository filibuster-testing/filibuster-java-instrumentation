package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcAmbiguousThrowAndErrorPropagationException} is invoked when the test indicates both throw and error
 * propagation: too ambiguous. To fix this exception, please verify you are only using either assertOnException(...) or assertFaultPropagates(...).
 */
public class FilibusterGrpcAmbiguousThrowAndErrorPropagationException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousThrowAndErrorPropagationException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousThrowAndErrorPropagationException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test indicates both throw and error propagation: too ambiguous.\nPlease verify you are only using either assertOnException(...) or assertFaultPropagates(...).";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Remove one use of matching:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultPropagates(io.grpc.MethodDescriptor)"),
                        generateSingleFixMessage(
                                "Remove one use of matching:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)")
                )
        );
    }
}
