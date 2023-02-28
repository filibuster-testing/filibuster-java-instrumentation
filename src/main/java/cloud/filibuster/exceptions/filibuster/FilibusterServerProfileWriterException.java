package cloud.filibuster.exceptions.filibuster;

public class FilibusterServerProfileWriterException extends FilibusterRuntimeException {
    public FilibusterServerProfileWriterException(String message) {
        super(message);
    }

    public FilibusterServerProfileWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterServerProfileWriterException(Throwable cause) {
        super(cause);
    }
}
