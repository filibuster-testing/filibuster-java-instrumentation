package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

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
