package cloud.filibuster.exceptions.filibuster;

public class FilibusterUnsupportedByHTTPServerException extends FilibusterRuntimeException {
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
