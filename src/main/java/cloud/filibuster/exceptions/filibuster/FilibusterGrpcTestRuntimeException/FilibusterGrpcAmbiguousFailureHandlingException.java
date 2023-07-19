package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcAmbiguousFailureHandlingException} indicates that the compositional verification failed due to ambiguous failure handling.
 * Each fault introduced has different impact.
 * To fix this exception, write an assertOnFaults(...) for this fault combination with appropriate assertions.
 */
public class FilibusterGrpcAmbiguousFailureHandlingException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousFailureHandlingException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousFailureHandlingException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Compositional verification failed due to ambiguous failure handling: each fault introduced has different impact. " +
                "\nPlease write an assertOnFaults(...) for this fault combination with appropriate assertions.";
    }
}
