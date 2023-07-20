package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import io.grpc.Status.Code;

/**
 * {@code FilibusterGrpcAssertionsForAssertOnExceptionFailedException} is invoked when assertions for assertOnException fail for a specific status code.
 * Please adjust assertOnException(...) for the assertions that should hold under this status code.
 */
public class FilibusterGrpcAssertionsForAssertOnExceptionFailedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertionsForAssertOnExceptionFailedException(Code code) {
        super(getErrorMessage(code));
    }

    public FilibusterGrpcAssertionsForAssertOnExceptionFailedException(Code code, Throwable cause) {
        super(getErrorMessage(code), cause);
    }

    private static String getErrorMessage(Code code) {
        return "Assertions for assertOnException failed.\nPlease adjust assertOnException(Status.Code." + code +
                ", Runnable) for the assertions that should hold under this status code.";
    }
}
