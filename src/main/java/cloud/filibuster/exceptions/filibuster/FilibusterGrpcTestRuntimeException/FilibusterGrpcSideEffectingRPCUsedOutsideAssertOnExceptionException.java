package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException} is thrown when sideEffectingRPC is used outside assertOnException(...) block. " +
 * Please rewrite code to specify precise assertions on mock invocations.
 */
public class FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of sideEffectingRPC not allowed outside of assertOnException(...) block.\nPlease rewrite code to specify precise assertions on mock invocations.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be used inside of exception assertion block:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)")
                )
        );
    }
}
