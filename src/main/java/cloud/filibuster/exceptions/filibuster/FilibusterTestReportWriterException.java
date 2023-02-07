package cloud.filibuster.exceptions.filibuster;

public class FilibusterTestReportWriterException extends FilibusterRuntimeException {
    public FilibusterTestReportWriterException(String message) {
        super(message);
    }

    public FilibusterTestReportWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterTestReportWriterException(Throwable cause) {
        super(cause);
    }
}
