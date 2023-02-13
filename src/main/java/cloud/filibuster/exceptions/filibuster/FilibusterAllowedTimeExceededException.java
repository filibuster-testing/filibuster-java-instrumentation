package cloud.filibuster.exceptions.filibuster;

public class FilibusterAllowedTimeExceededException extends FilibusterRuntimeException {
    public FilibusterAllowedTimeExceededException(String message) {
        super(message);
    }

    public FilibusterAllowedTimeExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterAllowedTimeExceededException(Throwable cause) {
        super(cause);
    }
}
