package cloud.filibuster.exceptions.filibuster;

public class FilibusterInstrumentationMissingDelegateException extends FilibusterRuntimeException {
    public FilibusterInstrumentationMissingDelegateException(String message) {
        super(message);
    }

    public FilibusterInstrumentationMissingDelegateException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterInstrumentationMissingDelegateException(Throwable cause) {
        super(cause);
    }
}
