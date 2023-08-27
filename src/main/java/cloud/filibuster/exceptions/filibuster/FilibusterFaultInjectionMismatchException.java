package cloud.filibuster.exceptions.filibuster;

public class FilibusterFaultInjectionMismatchException extends FilibusterRuntimeException {
    public FilibusterFaultInjectionMismatchException(String message) {
        super(message);
    }

    public FilibusterFaultInjectionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterFaultInjectionMismatchException(Throwable cause) {
        super(cause);
    }
}
