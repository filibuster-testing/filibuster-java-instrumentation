package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException} is invoked when test throws an exception, but no specification of failure behavior is present.
 * Please use assertFaultThrows(...) to specify failure is expected when fault is injected on this method, request or code.
 */
public class FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test threw an exception, but no specification of failure behavior present. \nUse assertFaultThrows(...) " +
                "to specify failure is expected when fault injected on this method, request or code.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(cloud.filibuster.junit.statem.CompositeFaultSpecification,io.grpc.Status.Code,java.lang.String)\">" +
                            "assertFaultThrows(CompositeFaultSpecification compositeFaultSpecification, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,io.grpc.Status.Code,java.lang.String)\">" +
                            "assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.String)\">" +
                            "assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,io.grpc.Status.Code,java.lang.String)\">" +
                            "assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, ReqT request, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,ReqT,io.grpc.Status.Code,java.lang.String)\">" +
                            "assertFaultThrows(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request, io.grpc.Status.Code thrownCode, java.lang.String thrownMessage)" +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
