package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

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
        return "Use of sideEffectingRPC not allowed outside of assertOnException(...) block. " +
                "Please rewrite code to specify precise assertions on mock invocations.";
    }
}
