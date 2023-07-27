package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

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
        return "Assertions in assertOnFault(...) failed.\nPlease adjust assertions in so that the test passes.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Adjust assertions in fault specification by combined fault specification:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(cloud.filibuster.junit.statem.CompositeFaultSpecification,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Adjust assertions in fault specification by RPC method, status code, and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Adjust assertions in fault specification  by RPC method and status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Adjust assertions in fault specification by RPC method and request:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,ReqT,java.lang.Runnable)"),
                        generateSingleFixMessage(
                                "Adjust assertions in fault specification by RPC method:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnFault(io.grpc.MethodDescriptor,java.lang.Runnable)")
                )
        );
    }
}
