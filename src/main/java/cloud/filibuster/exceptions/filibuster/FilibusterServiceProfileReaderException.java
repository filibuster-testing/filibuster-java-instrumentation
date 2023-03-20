package cloud.filibuster.exceptions.filibuster;

public class FilibusterServiceProfileReaderException extends FilibusterRuntimeException {
    public FilibusterServiceProfileReaderException(String message) {
        super(message);
    }

    public FilibusterServiceProfileReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterServiceProfileReaderException(Throwable cause) {
        super(cause);
    }
}
