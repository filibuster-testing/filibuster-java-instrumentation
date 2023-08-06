package cloud.filibuster.exceptions.transformer;

public class TransformerRuntimeException extends RuntimeException {
    public TransformerRuntimeException() {
        super();
    }
    public TransformerRuntimeException(String message) {
        super(message);
    }

    public TransformerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    protected TransformerRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public TransformerRuntimeException(Throwable cause) {
        super(cause);
    }
}
