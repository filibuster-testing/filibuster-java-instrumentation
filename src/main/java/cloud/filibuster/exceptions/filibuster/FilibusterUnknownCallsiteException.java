package cloud.filibuster.exceptions.filibuster;

public class FilibusterUnknownCallsiteException extends RuntimeException {
    public FilibusterUnknownCallsiteException(String message) {
        super(message);
    }

    public FilibusterUnknownCallsiteException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterUnknownCallsiteException(Throwable cause) {
        super(cause);
    }
}
