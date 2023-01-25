package cloud.filibuster.exceptions.filibuster;

public class FilibusterDivergenceException extends RuntimeException {
    public FilibusterDivergenceException(String message) {
        super(message);
    }

    public FilibusterDivergenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterDivergenceException(Throwable cause) {
        super(cause);
    }
}
