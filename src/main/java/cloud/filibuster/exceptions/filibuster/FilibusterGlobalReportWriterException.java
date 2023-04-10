package cloud.filibuster.exceptions.filibuster;

public class FilibusterGlobalReportWriterException  extends FilibusterRuntimeException {
    public FilibusterGlobalReportWriterException(String message) {
        super(message);
    }

    public FilibusterGlobalReportWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterGlobalReportWriterException(Throwable cause) {
        super(cause);
    }
}
