package cloud.filibuster.exceptions.filibuster;

public class FilibusterCallsiteGenerationException extends FilibusterRuntimeException {
    public FilibusterCallsiteGenerationException(String message) {
        super(message);
    }

    public FilibusterCallsiteGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterCallsiteGenerationException(Throwable cause) {
        super(cause);
    }
}
