package cloud.filibuster.exceptions.filibuster;

public class FilibusterMessageSerializationException extends FilibusterRuntimeException {
    public FilibusterMessageSerializationException(String message) {
        super(message);
    }

    public FilibusterMessageSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterMessageSerializationException(Throwable cause) {
        super(cause);
    }
}
