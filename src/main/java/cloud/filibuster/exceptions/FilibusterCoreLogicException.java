package cloud.filibuster.exceptions;

public class FilibusterCoreLogicException extends RuntimeException {
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
