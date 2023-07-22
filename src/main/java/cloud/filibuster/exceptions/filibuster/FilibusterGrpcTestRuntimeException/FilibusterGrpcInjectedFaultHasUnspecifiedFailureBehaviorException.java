package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

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
        return "Test injected a fault, but no specification of failure behavior present.\nPlease use assertOnFault(...) or assertFaultHasNoImpact(...) to specify assertions under fault.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Specify fault results in different assertions by combined fault specification:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(cloud.filibuster.junit.statem.CompositeFaultSpecification,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Specify fault results in different assertions by RPC method, status code, and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Specify fault results in different assertions by RPC method and status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Specify fault results in different assertions by RPC method and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,ReqT,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Specify fault results in different assertions by RPC method:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Specify fault has no impact by RPC method, status code, and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT)"),
                        generateSingleFixMessage(
                                "Specify fault has no impact by RPC method and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor,ReqT)"),
                        generateSingleFixMessage(
                                "Specify fault has no impact by RPC method and status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor,io.grpc.Status.Code)"),
                        generateSingleFixMessage(
                                "Specify fault has no impact by RPC method:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultHasNoImpact(io.grpc.MethodDescriptor)")
                )
        );
    }
}
