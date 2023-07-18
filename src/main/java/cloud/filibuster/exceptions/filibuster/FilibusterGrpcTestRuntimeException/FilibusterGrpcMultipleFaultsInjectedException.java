package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

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
}
