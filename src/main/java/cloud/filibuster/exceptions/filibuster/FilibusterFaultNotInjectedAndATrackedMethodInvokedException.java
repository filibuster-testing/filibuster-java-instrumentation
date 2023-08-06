package cloud.filibuster.exceptions.filibuster;

public class FilibusterFaultNotInjectedAndATrackedMethodInvokedException extends FilibusterRuntimeException {
    public FilibusterFaultNotInjectedAndATrackedMethodInvokedException(String message) {
        super(message);
    }

    public FilibusterFaultNotInjectedAndATrackedMethodInvokedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterFaultNotInjectedAndATrackedMethodInvokedException(Throwable cause) {
        super(cause);
    }
}
