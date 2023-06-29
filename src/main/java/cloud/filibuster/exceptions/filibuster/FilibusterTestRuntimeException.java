package cloud.filibuster.exceptions.filibuster;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

public class FilibusterTestRuntimeException extends FilibusterRuntimeException {
    public FilibusterTestRuntimeException(String message) {
        super(message);
    }

    public FilibusterTestRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilibusterTestRuntimeException(Throwable cause) {
        super(cause);
    }
}
