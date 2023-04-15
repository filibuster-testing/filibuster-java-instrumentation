package cloud.filibuster.exceptions.filibuster;

public class FilibusterServerNullException extends FilibusterRuntimeException {
    public FilibusterServerNullException(String message) {
        super(message);
    }

    public FilibusterServerNullException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterServerNullException(Throwable cause) {
        super(cause);
    }
}
