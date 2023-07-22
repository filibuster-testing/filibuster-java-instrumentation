package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException} is thrown when readOnlyRPC is used outside assertOnException(...) block. " +
 * Please rewrite code to specify precise assertions on mock invocations.
 */
public class FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of readOnlyRPC not allowed outside of assertOnException(...) block.\nPlease rewrite code to specify precise assertions on mock invocations.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Use of readOnlyRPC assertion must be used inside of exception assertion block:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)")
                )
        );
    }
}
