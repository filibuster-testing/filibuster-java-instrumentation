package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import io.grpc.Status.Code;

/**
 * {@code FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException} is invoked when the assertions do not hold under
 * error response of a specific status code.
 * To fix this exception, please adjust assertOnException(...) for the assertions that should hold under this status code.
 */
public class FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException(Code code) {
        super(getErrorMessage(code));
    }

    public FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException(Code code, Throwable cause) {
        super(getErrorMessage(code), cause);
    }

    private static String getErrorMessage(Code code) {
        return "Assertions did not hold under error response." +
                "Please adjust assertOnException(Status.Code." + code + ", Runnable) for the assertions that should hold under this status code.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertTestBlock()\">" +
                            "Place test assertions in assertTestBlock instead." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
