package cloud.filibuster.exceptions.filibuster;

public class FilibusterServiceProfileWriterException extends FilibusterRuntimeException {
    public FilibusterServiceProfileWriterException(String message) {
        super(message);
    }

    public FilibusterServiceProfileWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterServiceProfileWriterException(Throwable cause) {
        super(cause);
    }
}
