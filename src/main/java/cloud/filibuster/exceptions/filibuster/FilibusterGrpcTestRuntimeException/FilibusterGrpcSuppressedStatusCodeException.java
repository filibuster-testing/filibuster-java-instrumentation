package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcSuppressedStatusCodeException} is thrown when injected fault's status code was suppressed, but test indicates this should propagate directly upstream.
 * Please ensure that use of assertFaultPropagates(...) is correct.
 */
public class FilibusterGrpcSuppressedStatusCodeException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcSuppressedStatusCodeException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcSuppressedStatusCodeException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Injected fault's status code was suppressed, but test indicates this should propagate directly upstream.\nEnsure that use of assertFaultPropagates(...) is correct.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Ensure correct use of: ",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultPropagates(io.grpc.MethodDescriptor)"
                        )
                )
        );
    }
}
