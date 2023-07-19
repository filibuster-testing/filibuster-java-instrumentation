package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException} is thrown when readOnlyRPC is used outside assertOnException(...) block. " +
 * Please rewrite code to specify precise assertions on mock invocations.
 */
public class FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of readOnlyRPC not allowed outside of assertOnException(...) block. " +
                "Please rewrite code to specify precise assertions on mock invocations.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertTestBlock()\">" +
                            "Potentially place assertions in assertTestBlock instead of assertOnException." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
