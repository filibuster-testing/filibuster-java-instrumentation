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
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.Runnable)\">" +
                            "Adjust assertions in assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,java.lang.Runnable)\">" +
                            "Adjust assertions in assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, ReqT request, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,java.lang.Runnable)\">" +
                            "Adjust assertions in assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,ReqT,java.lang.Runnable)\">" +
                            "Adjust assertions in assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
