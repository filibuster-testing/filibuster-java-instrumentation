package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import io.grpc.Status.Code;

public class FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException(Code code) {
        super(getErrorMessage(code));
    }

    public FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException(Code code, Throwable cause) {
        super(getErrorMessage(code), cause);
    }

    private static String getErrorMessage(Code code) {
        return "Assertions did not hold under error response." +
        "Please adjust assertOnException(Status.Code." + code + ", Runnable) for the assertions that should hold under this status code.";
    }
}
