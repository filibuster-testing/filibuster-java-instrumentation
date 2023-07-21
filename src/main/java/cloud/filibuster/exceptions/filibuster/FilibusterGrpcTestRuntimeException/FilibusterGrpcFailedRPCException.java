package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

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
        return "Failed RPC resulted in exception, but error codes and descriptions did not match.\nVerify assertFaultThrows(...) and thrown exception match.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Verify status code and description matches for thrown exception by combined fault specification: ",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(cloud.filibuster.junit.statem.CompositeFaultSpecification,io.grpc.Status.Code,java.lang.String)"
                        ),
                        generateSingleFixMessage(
                                "Verify status code and description matches for thrown exception by RPC method, status code, and request: ",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,ReqT,io.grpc.Status.Code,java.lang.String)"
                        ),
                        generateSingleFixMessage(
                                "Verify status code and description matches for thrown exception by RPC method and status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,io.grpc.Status.Code,java.lang.String)"
                        ),
                        generateSingleFixMessage(
                                "Verify status code and description matches for thrown exception by RPC method and request: ",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,ReqT,io.grpc.Status.Code,java.lang.String)"
                        ),
                        generateSingleFixMessage(
                                "Verify status code and description matches for thrown exception by RPC method:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertFaultThrows(io.grpc.MethodDescriptor,io.grpc.Status.Code,java.lang.String)"
                        )
                )
        );
    }
}
