package cloud.filibuster.exceptions.filibuster;

public class FilibusterFaultInjectionException extends RuntimeException {
    public FilibusterFaultInjectionException(String message) {
        super(message);
    }

    public FilibusterFaultInjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterFaultInjectionException(Throwable cause) {
        super(cause);
    }
}
