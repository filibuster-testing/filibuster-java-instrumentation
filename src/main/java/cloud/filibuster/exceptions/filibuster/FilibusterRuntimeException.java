package cloud.filibuster.exceptions.filibuster;

public class FilibusterRuntimeException extends RuntimeException {
    public FilibusterRuntimeException() {
        super();
    }
    public FilibusterRuntimeException(String message) {
        super(message);
    }

    public FilibusterRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    protected FilibusterRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public FilibusterRuntimeException(Throwable cause) {
        super(cause);
    }
}
