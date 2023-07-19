package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcMultipleFaultsInjectedException} is thrown when assertions in assertTestBlock fail due to multiple faults being injected.
 * Please use assertOnFault to update assertions so that they hold under fault.
 */
public class FilibusterGrpcMultipleFaultsInjectedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcMultipleFaultsInjectedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcMultipleFaultsInjectedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertTestBlock failed due to multiple faults being injected. \nPlease use assertOnFault to update assertions so that they hold under fault.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.Runnable)\">" +
                            "Update assertions so that they hold under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,java.lang.Runnable)\">" +
                            "Update assertions so that they hold under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, io.grpc.Status.Code code, ReqT request, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,java.lang.Runnable)\">" +
                            "Update assertions so that they hold under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,ReqT,java.lang.Runnable)\">" +
                            "Update assertions so that they hold under fault using assertOnFault(io.grpc.MethodDescriptor<ReqT,ResT> methodDescriptor, ReqT request, java.lang.Runnable runnable)." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
