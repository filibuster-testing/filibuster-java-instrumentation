package cloud.filibuster.junit.exceptions;

import cloud.filibuster.exceptions.FilibusterRuntimeException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NoopException extends FilibusterRuntimeException {
    private static final Logger logger = Logger.getLogger(NoopException.class.getName());

    public static void logAndThrow(String message) {
        logger.log(Level.SEVERE, message);
        throw new NoopException(message);
    }

    public NoopException(String message) {
        super(message);
    }
}
