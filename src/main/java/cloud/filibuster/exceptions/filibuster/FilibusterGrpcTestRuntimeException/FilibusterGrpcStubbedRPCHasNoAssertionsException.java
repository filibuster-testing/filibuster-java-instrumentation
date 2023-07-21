package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcStubbedRPCHasNoAssertionsException} is thrown when stubbed RPC has no assertions on invocation count.
 * Please use verifyThat to specify expected invocation count.
 */
public class FilibusterGrpcStubbedRPCHasNoAssertionsException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcStubbedRPCHasNoAssertionsException(String key) {
        super(getErrorMessage(key));
    }

    public FilibusterGrpcStubbedRPCHasNoAssertionsException(String key, Throwable cause) {
        super(getErrorMessage(key), cause);
    }

    private static String getErrorMessage(String key) {
        return "Stubbed RPC " + key + " has no assertions on invocation count.\nUse verifyThat to specify expected invocation count.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Set an expectation that a stub will be invoked a particular number of times:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/GrpcMock.html#verifyThat(io.grpc.MethodDescriptor,int)"
                        ),
                        generateSingleFixMessage(
                                "Set an expectation that a stub will be invoked, with a given request, a particular number of times:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/GrpcMock.html#verifyThat(io.grpc.MethodDescriptor,ReqT,int)"
                        )
                )
        );
    }
}
