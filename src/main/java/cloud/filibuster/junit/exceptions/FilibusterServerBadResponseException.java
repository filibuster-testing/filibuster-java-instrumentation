package cloud.filibuster.junit.exceptions;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FilibusterServerBadResponseException extends FilibusterRuntimeException {
    private static final Logger logger = Logger.getLogger(FilibusterServerBadResponseException.class.getName());

    public static void logAndThrow(String message) {
        logger.log(Level.SEVERE, message);
        throw new FilibusterServerBadResponseException(message);
    }

    public FilibusterServerBadResponseException(String message) {
        super(message);
    }
}
