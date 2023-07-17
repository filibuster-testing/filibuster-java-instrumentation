package cloud.filibuster.exceptions.filibuster;

public class FilibusterFaultNotInjectedException extends FilibusterRuntimeException {
    public FilibusterFaultNotInjectedException(String message) {
        super(message);
    }

    public FilibusterFaultNotInjectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterFaultNotInjectedException(Throwable cause) {
        super(cause);
    }
}
