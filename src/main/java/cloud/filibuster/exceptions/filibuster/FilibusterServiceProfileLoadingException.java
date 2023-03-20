package cloud.filibuster.exceptions.filibuster;

public class FilibusterServiceProfileLoadingException extends FilibusterRuntimeException {
    public FilibusterServiceProfileLoadingException(String message) {
        super(message);
    }

    public FilibusterServiceProfileLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterServiceProfileLoadingException(Throwable cause) {
        super(cause);
    }
}
