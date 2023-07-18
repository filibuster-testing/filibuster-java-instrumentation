package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import io.grpc.Status.Code;

public class FilibusterGrpcMissingAssertionForStatusCodeException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcMissingAssertionForStatusCodeException(Code code) {
        super(getErrorMessage(code));
    }

    public FilibusterGrpcMissingAssertionForStatusCodeException(Code code, Throwable cause) {
        super(getErrorMessage(code), cause);
    }

    private static String getErrorMessage(Code code) {
        return "Missing assertion block for Status.Code." + code + " response. " +
                "Please write assertOnException(Status.Code." +
                code + ", Runnable) for the assertions that should hold under this status code.";
    }
}
