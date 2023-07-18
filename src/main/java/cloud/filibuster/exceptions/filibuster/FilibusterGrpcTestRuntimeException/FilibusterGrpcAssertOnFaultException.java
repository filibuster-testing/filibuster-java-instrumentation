package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcAssertOnFaultException} is invoked when the assertions in assertOnFault(...) fail.
 * Please adjust assertions in so that the test passes.
 */
public class FilibusterGrpcAssertOnFaultException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertOnFaultException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAssertOnFaultException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertOnFault(...) failed. Please adjust assertions in so that the test passes.";
    }
}
