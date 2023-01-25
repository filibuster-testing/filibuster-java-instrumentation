package cloud.filibuster.exceptions.filibuster;

public class FilibusterUnsupportedByHTTPServerException extends RuntimeException {
    public FilibusterUnsupportedByHTTPServerException(String message) {
        super(message);
    }

    public FilibusterUnsupportedByHTTPServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterUnsupportedByHTTPServerException(Throwable cause) {
        super(cause);
    }
}
