package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException} is thrown when sideEffectingRPC is used outside assertOnException(...) and assertOnFault(...) block.
 * Please rewrite code to specify precise assertions on mock invocations.
 */
public class FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of sideEffectingRPC is allowed either in assertOnException(...) or assertOnFault(...) block.\nPlease rewrite code to specify precise assertions on mock invocations.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be inside of exception assertion block:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)"
                        ), generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be inside of fault assertion block by specifying combined fault specification:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(cloud.filibuster.junit.statem.CompositeFaultSpecification,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be inside of fault assertion block by specifying RPC method, status code, and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be inside of fault assertion block by specifying RPC method and status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be inside of fault assertion block by specifying RPC method and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,ReqT,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Use of sideEffectingRPC assertion must be inside of fault assertion block by specifying RPC method:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,java.lang.Runnable)")
                )
        );
    }
}
