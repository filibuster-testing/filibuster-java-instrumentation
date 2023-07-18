package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

public class FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test injected a fault, but no specification of failure behavior present. Please use assertOnFault(...) or assertFaultHasNoImpact(...) to specify assertions under fault.";
    }
}
