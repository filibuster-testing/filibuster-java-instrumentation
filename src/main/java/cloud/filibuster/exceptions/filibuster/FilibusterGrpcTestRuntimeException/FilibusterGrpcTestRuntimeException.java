package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

/**
 * {@code FilibusterGrpcTestRuntimeException} is the abstract superclass of the
 * exceptions that can be thrown from the GRPC test interface.
 *
 * <p>{@code FilibusterGrpcTestRuntimeException} and its subclasses are <em>unchecked
 * exceptions</em>.
 */
public abstract class FilibusterGrpcTestRuntimeException extends FilibusterRuntimeException {

    /**
     * Constructs a new Filibuster GRPC runtime exception with the specified detail message.
     *
     * @param message the detail message.
     */
    protected FilibusterGrpcTestRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a new Filibuster GRPC runtime exception with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    protected FilibusterGrpcTestRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the fix message for the exception.
     */
    abstract protected String getFixMessage();
}
