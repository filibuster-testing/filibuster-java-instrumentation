package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

public class FilibusterGrpcAmbiguousFailureHandlingException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousFailureHandlingException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousFailureHandlingException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Compositional verification failed due to ambiguous failure handling: each fault introduced has different impact. " +
                "Please write an assertOnFaults(...) for this fault combination with appropriate assertions.";
    }
}
