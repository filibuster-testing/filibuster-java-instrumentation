package cloud.filibuster.exceptions.filibuster;

public class FilibusterServerProfileReaderException extends FilibusterRuntimeException {
    public FilibusterServerProfileReaderException(String message) {
        super(message);
    }

    public FilibusterServerProfileReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterServerProfileReaderException(Throwable cause) {
        super(cause);
    }
}
