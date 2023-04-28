package cloud.filibuster.exceptions.filibuster;

public class FilibusterUnsupportedAPIException extends FilibusterRuntimeException {
    public FilibusterUnsupportedAPIException(String message) {
        super(message);
    }

    public FilibusterUnsupportedAPIException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterUnsupportedAPIException(Throwable cause) {
        super(cause);
    }
}
