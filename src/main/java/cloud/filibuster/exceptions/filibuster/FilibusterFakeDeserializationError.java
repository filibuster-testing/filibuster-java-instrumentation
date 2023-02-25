package cloud.filibuster.exceptions.filibuster;

public class FilibusterFakeDeserializationError extends FilibusterRuntimeException {
    public FilibusterFakeDeserializationError(String message) {
        super(message);
    }

    public FilibusterFakeDeserializationError(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterFakeDeserializationError(Throwable cause) {
        super(cause);
    }
}
