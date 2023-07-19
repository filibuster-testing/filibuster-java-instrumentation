package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcFailedRPCException} is invoked when a failed RPC results in exception, but error codes and descriptions do not match.
 * Please verify assertFaultThrows(...) and thrown exception match.
 */
public class FilibusterGrpcFailedRPCException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcFailedRPCException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcFailedRPCException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Failed RPC resulted in exception, but error codes and descriptions did not match. \nVerify assertFaultThrows(...) and thrown exception match.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(cloud.filibuster.junit.statem.CompositeFaultSpecification,io.grpc.Status.Code,java.lang.String)\">" +
                            "Verify thrown exception matches assertFaultThrows(CompositeFaultSpecification compositeFaultSpecification, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,io.grpc.Status.Code,java.lang.String)\">" +
                            "Verify thrown exception matches assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.String)\">" +
                            "Verify thrown exception matches assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,io.grpc.Status.Code,java.lang.String)\">" +
                            "Verify thrown exception matches assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, ReqT request, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,ReqT,io.grpc.Status.Code,java.lang.String)\">" +
                            "Verify thrown exception matches assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
