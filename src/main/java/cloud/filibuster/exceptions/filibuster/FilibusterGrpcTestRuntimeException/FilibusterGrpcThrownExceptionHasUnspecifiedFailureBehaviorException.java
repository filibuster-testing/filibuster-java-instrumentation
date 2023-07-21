package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

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
        return "Test threw an exception, but no specification of failure behavior present.\nUse assertFaultThrows(...) to specify failure is expected when fault injected on this method, request or code.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Specify failure is expected when fault is injected for a combination of faults:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(cloud.filibuster.junit.statem.CompositeFaultSpecification,io.grpc.Status.Code,java.lang.String)"),
                        generateSingleFixMessage(
                                "Specify failure is expected when fault is injected on a single RPC method with a particular status code and specific request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,io.grpc.Status.Code,java.lang.String)"),
                        generateSingleFixMessage(
                                "Specify failure is expected when fault is injected on a single RPC method with a particular status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,io.grpc.Status.Code,java.lang.String)"),
                        generateSingleFixMessage(
                                "Specify failure is expected when fault is injected on a single RPC method:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.String)"),
                        generateSingleFixMessage(
                                "Specify failure propagates to upstream because it is not caught:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultPropagates(io.grpc.MethodDescriptor)")
                )
        );
    }
}
