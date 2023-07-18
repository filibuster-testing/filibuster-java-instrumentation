package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

public abstract class FilibusterGrpcTestRuntimeException extends FilibusterRuntimeException {
    protected FilibusterGrpcTestRuntimeException(String message) {
        super(message);
    }
    protected FilibusterGrpcTestRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
