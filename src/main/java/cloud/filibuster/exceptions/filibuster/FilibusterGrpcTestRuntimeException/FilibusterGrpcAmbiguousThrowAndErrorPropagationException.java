package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcAmbiguousThrowAndErrorPropagationException} is invoked when the test indicates both throw and error
 * propagation: too ambiguous. To fix this exception, please verify you are only using either assertOnException(...) or assertFaultPropagates(...).
 */
public class FilibusterGrpcAmbiguousThrowAndErrorPropagationException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousThrowAndErrorPropagationException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousThrowAndErrorPropagationException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test indicates both throw and error propagation: too ambiguous.\n" +
               "Please verify you are only using either assertOnException(...) or assertFaultPropagates(...).";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertTestBlock()\">" +
                            "Place test assertions in assertTestBlock.\n." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
