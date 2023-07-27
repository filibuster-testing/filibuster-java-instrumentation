package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import io.grpc.Status.Code;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcAssertionsForAssertOnExceptionFailedException} is invoked when assertions for assertOnException fail for a specific status code.
 * Please adjust assertOnException(...) for the assertions that should hold under this status code.
 */
public class FilibusterGrpcAssertionsForAssertOnExceptionFailedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertionsForAssertOnExceptionFailedException(Code code) {
        super(getErrorMessage(code));
    }

    public FilibusterGrpcAssertionsForAssertOnExceptionFailedException(Code code, Throwable cause) {
        super(getErrorMessage(code), cause);
    }

    private static String getErrorMessage(Code code) {
        return "Assertions for assertOnException failed.\nPlease adjust assertOnException(Status.Code." + code + ", Runnable) for the assertions that should hold under this status code.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Fix assertions for current thrown exception with status code:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertOnException(io.grpc.Status.Code,java.lang.Runnable)"
                        )
                )
        );
    }
}
