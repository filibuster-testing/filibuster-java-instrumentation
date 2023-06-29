package cloud.filibuster.exceptions.transformer;

public class TransformerNullResultException extends TransformerRuntimeException {
    public TransformerNullResultException(String message) {
        super(message);
    }

    public TransformerNullResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformerNullResultException(Throwable cause) {
        super(cause);
    }
}
