package cloud.filibuster.exceptions.filibuster;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FilibusterNoopException extends FilibusterRuntimeException {
    private static final Logger logger = Logger.getLogger(FilibusterNoopException.class.getName());

    public static void logAndThrow(String message) {
        logger.log(Level.SEVERE, message);
        throw new FilibusterNoopException(message);
    }

    public FilibusterNoopException(String message) {
        super(message);
    }
}
