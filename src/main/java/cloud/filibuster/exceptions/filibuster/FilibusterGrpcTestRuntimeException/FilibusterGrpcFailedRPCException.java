package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcFailedRPCException} is invoked when a failed RPC results in exception, but error codes and descriptions do not match.
 * Please verify assertFaultThrows(...) and thrown exception match.
 */
public class FilibusterGrpcFailedRPCException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcFailedRPCException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcFailedRPCException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Failed RPC resulted in exception, but error codes and descriptions did not match. Verify assertFaultThrows(...) and thrown exception match.";
    }

    @Override
    public String getFixMessage() {
        return null;
    }
}
