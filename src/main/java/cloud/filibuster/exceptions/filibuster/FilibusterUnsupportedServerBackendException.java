package cloud.filibuster.exceptions.filibuster;

public class FilibusterUnsupportedServerBackendException extends FilibusterRuntimeException {
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
