package cloud.filibuster.exceptions.filibuster;

public class FilibusterDeserializationError extends FilibusterRuntimeException {
    public FilibusterDeserializationError(String message) {
        super(message);
    }

    public FilibusterDeserializationError(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterDeserializationError(Throwable cause) {
        super(cause);
    }
}
