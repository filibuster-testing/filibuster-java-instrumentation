package cloud.filibuster.exceptions.filibuster;

public class FilibusterCoreLogicException extends FilibusterRuntimeException {
    public FilibusterCoreLogicException(String message) {
        super(message);
    }

    public FilibusterCoreLogicException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterCoreLogicException(Throwable cause) {
        super(cause);
    }
}
