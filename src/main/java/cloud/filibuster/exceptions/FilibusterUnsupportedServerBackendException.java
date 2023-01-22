package cloud.filibuster.exceptions;

public class FilibusterUnsupportedServerBackendException extends RuntimeException {
    public FilibusterUnsupportedServerBackendException(String message) {
        super(message);
    }

    public FilibusterUnsupportedServerBackendException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterUnsupportedServerBackendException(Throwable cause) {
        super(cause);
    }
}
