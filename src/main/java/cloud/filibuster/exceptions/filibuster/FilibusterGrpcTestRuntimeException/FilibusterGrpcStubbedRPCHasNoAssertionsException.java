package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcStubbedRPCHasNoAssertionsException} is thrown when stubbed RPC has no assertions on invocation count.
 * Please use verifyThat to specify expected invocation count.
 */
public class FilibusterGrpcStubbedRPCHasNoAssertionsException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcStubbedRPCHasNoAssertionsException(String key) {
        super(getErrorMessage(key));
    }

    public FilibusterGrpcStubbedRPCHasNoAssertionsException(String key, Throwable cause) {
        super(getErrorMessage(key), cause);
    }

    private static String getErrorMessage(String key) {
        return "Stubbed RPC " + key + " has no assertions on invocation count. " +
                "Use verifyThat to specify expected invocation count.";
    }

    @Override
    protected String getFixMessage() {
        return null;
    }
}
