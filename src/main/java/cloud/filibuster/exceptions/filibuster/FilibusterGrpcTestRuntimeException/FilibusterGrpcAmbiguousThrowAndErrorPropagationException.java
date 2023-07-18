package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcAmbiguousThrowAndErrorPropagationException} is invoked when the test indicates both throw and error
 * propagation: too ambiguous. To fix this exception, please verify you are only using either assertOnException(...) or assertFaultPropagates(...).
 */
public class FilibusterGrpcAmbiguousThrowAndErrorPropagationException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousThrowAndErrorPropagationException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousThrowAndErrorPropagationException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test indicates both throw and error propagation: too ambiguous. Please verify you are only using either " +
                "assertOnException(...) or assertFaultPropagates(...).";
    }
}
