package cloud.filibuster.exceptions.filibuster;

public class FilibusterOrganicFailuresPresentException extends FilibusterRuntimeException {
    public FilibusterOrganicFailuresPresentException(String message) {
        super(message);
    }

    public FilibusterOrganicFailuresPresentException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterOrganicFailuresPresentException(Throwable cause) {
        super(cause);
    }
}
