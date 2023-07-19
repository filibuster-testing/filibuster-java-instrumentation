package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcAssertOnFaultException} is invoked when the assertions in assertOnFault(...) fail.
 * Please adjust assertions in so that the test passes.
 */
public class FilibusterGrpcAssertOnFaultException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertOnFaultException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAssertOnFaultException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertOnFault(...) failed. \nPlease adjust assertions in so that the test passes.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertStubBlock()\">" +
                            "Use the test assertStubBlock for performing assertions on stub invocations." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertTestBlock()\">" +
                            "Place test assertion in assertTestBlock." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
