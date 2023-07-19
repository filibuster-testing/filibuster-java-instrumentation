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
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#failureBlock()\">" +
                            "Place failure specification in failureBlock." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
