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
                "\nPlease rewrite code to specify precise assertions on mock invocations.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)\">" +
                            "Rewrite your code to use readOnlyRPC in assertOnException(io.grpc.Status.Code code, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#readOnlyRPC(io.grpc.MethodDescriptor)\">" +
                            "Use readOnlyRPC(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor) only inside assertOnException." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#readOnlyRPC(io.grpc.MethodDescriptor,ReqT)\">" +
                            "Use readOnlyRPC(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request) only inside assertOnException." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
