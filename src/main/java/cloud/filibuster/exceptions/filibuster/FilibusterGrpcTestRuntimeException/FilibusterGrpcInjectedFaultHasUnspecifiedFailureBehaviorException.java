package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException} is invoked when a test injects a fault, but no specification of failure behavior is present.
 * Please use assertOnFault(...) or assertFaultHasNoImpact(...) to specify assertions under fault.
 */
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

    @Override
    protected String getFixMessage() {
        return null;
    }
}
