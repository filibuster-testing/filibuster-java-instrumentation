package cloud.filibuster.exceptions.vector_clock;

public class VectorClockCloneException extends RuntimeException {
    public VectorClockCloneException(String message) {
        super(message);
    }

    public VectorClockCloneException(String message, Throwable cause) {
        super(message, cause);
    }

    public VectorClockCloneException(Throwable cause) {
        super(cause);
    }
}
