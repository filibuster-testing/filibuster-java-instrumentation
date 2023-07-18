package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcMultipleFaultsInjectedException} is thrown when assertions in assertTestBlock fail due to multiple faults being injected.
 * Please use assertOnFault to update assertions so that they hold under fault.
 */
public class FilibusterGrpcMultipleFaultsInjectedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcMultipleFaultsInjectedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcMultipleFaultsInjectedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertTestBlock failed due to multiple faults being injected. Please use assertOnFault to update assertions so that they hold under fault.";
    }

    @Override
    protected String getFixMessage() {
        return null;
    }
}
