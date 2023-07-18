package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

public class FilibusterGrpcSuppressedStatusCodeException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcSuppressedStatusCodeException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcSuppressedStatusCodeException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Injected fault's status code was suppressed, but test indicates this should propagate directly upstream. " +
                "Ensure that use of assertFaultPropagates(...) is correct.";
    }
}
