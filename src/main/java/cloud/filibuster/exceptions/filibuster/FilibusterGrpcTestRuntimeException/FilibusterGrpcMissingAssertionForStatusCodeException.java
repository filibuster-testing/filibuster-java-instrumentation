package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import io.grpc.Status.Code;

/**
 * {@code FilibusterGrpcMissingAssertionForStatusCodeException} is thrown when assertion block for a specific status code response is missing. "
 * Please write assertOnException(..) for the assertions that should hold under this status code.
 */
public class FilibusterGrpcMissingAssertionForStatusCodeException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcMissingAssertionForStatusCodeException(Code code) {
        super(getErrorMessage(code));
    }

    public FilibusterGrpcMissingAssertionForStatusCodeException(Code code, Throwable cause) {
        super(getErrorMessage(code), cause);
    }

    private static String getErrorMessage(Code code) {
        return "Missing assertion block for Status.Code." + code + " response. " + "\nPlease write assertOnException(Status.Code." +
                code + ", Runnable) for the assertions that should hold under this status code.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)\">" +
                            "Write assertOnException for the assertions that should hold under the current status code." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
