package cloud.filibuster.exceptions.filibuster;

public class FilibusterLatencyInjectionException extends FilibusterRuntimeException {
    public FilibusterLatencyInjectionException(String message) {
        super(message);
    }

    public FilibusterLatencyInjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterLatencyInjectionException(Throwable cause) {
        super(cause);
    }
}
