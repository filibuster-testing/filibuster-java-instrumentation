package cloud.filibuster.exceptions.filibuster;

public class FilibusterUnsupportedCustomAnalysisFileException extends FilibusterRuntimeException {
    public FilibusterUnsupportedCustomAnalysisFileException(String message) {
        super(message);
    }

    public FilibusterUnsupportedCustomAnalysisFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterUnsupportedCustomAnalysisFileException(Throwable cause) {
        super(cause);
    }
}
