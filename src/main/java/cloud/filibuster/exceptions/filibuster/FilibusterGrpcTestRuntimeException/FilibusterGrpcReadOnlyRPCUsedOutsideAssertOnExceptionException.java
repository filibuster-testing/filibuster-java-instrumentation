package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

public class FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of readOnlyRPC not allowed outside of assertOnException(...) block. " +
                "Please rewrite code to specify precise assertions on mock invocations.";
    }
}
