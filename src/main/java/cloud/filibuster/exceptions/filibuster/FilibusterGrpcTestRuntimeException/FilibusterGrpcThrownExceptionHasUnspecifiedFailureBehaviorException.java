package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException} is invoked when test throws an exception, but no specification of failure behavior is present.
 * Please use assertFaultThrows(...) to specify failure is expected when fault is injected on this method, request or code.
 */
public class FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test threw an exception, but no specification of failure behavior present. Use assertFaultThrows(...) " +
                "to specify failure is expected when fault injected on this method, request or code.";
    }

    @Override
    public String getFixMessage() {
        return null;
    }
}
