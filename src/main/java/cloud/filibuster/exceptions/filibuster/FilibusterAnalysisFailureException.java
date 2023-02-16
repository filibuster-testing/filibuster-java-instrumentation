package cloud.filibuster.exceptions.filibuster;

public class FilibusterAnalysisFailureException extends FilibusterRuntimeException {
    public FilibusterAnalysisFailureException(String message) {
        super(message);
    }

    public FilibusterAnalysisFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterAnalysisFailureException(Throwable cause) {
        super(cause);
    }
}
