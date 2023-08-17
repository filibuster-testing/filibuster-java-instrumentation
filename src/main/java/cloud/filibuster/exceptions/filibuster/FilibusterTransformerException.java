package cloud.filibuster.exceptions.filibuster;

public class FilibusterTransformerException extends FilibusterRuntimeException {
    public FilibusterTransformerException(String message) {
        super(message);
    }

    public FilibusterTransformerException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterTransformerException(Throwable cause) {
        super(cause);
    }
}
