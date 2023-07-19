package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException} is thrown when sideEffectingRPC is used outside assertOnException(...) block. " +
 * Please rewrite code to specify precise assertions on mock invocations.
 */
public class FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of sideEffectingRPC not allowed outside of assertOnException(...) block. " +
                "\nPlease rewrite code to specify precise assertions on mock invocations.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)\">" +
                            "assertOnException(io.grpc.Status.Code code, java.lang.Runnable runnable)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#sideEffectingRPC(io.grpc.MethodDescriptor,int)\">" +
                            "sideEffectingRPC(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, int count)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#sideEffectingRPC(io.grpc.MethodDescriptor,ReqT,int)\">" +
                            "sideEffectingRPC(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request, int count)" +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
