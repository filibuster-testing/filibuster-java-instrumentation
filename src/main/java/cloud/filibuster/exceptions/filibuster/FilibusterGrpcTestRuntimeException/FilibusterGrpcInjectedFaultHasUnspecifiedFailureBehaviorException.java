package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException} is invoked when a test injects a fault, but no specification of failure behavior is present.
 * Please use assertOnFault(...) or assertFaultHasNoImpact(...) to specify assertions under fault.
 */
public class FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Test injected a fault, but no specification of failure behavior present. \nPlease use assertOnFault(...) or assertFaultHasNoImpact(...) to specify assertions under fault.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.Runnable)\">" +
                            "Specify assertions under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,java.lang.Runnable)\">" +
                            "Specify assertions under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, ReqT request, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,java.lang.Runnable)\">" +
                            "Specify assertions under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,ReqT,java.lang.Runnable)\">" +
                            "Specify assertions under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor)\">" +
                            "Specify assertions under fault using assertFaultHasNoImpact(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor,io.grpc.Status.Code)\">" +
                            "Specify assertions under fault using assertFaultHasNoImpact(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT)\">" +
                            "Specify assertions under fault using assertFaultHasNoImpact(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, ReqT request)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor,ReqT)\">" +
                            "Specify assertions under fault using assertFaultHasNoImpact(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request)." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
