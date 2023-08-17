package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcVerifyThatUsedOutsideAssertStubException} is thrown when verifyThat is used outside assertStubBlock(...). " +
 * Please rewrite code to place verifyThat in the assertStubBlock(...).
 */
public class FilibusterGrpcVerifyThatUsedOutsideAssertStubException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcVerifyThatUsedOutsideAssertStubException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcVerifyThatUsedOutsideAssertStubException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of verifyThat not allowed outside of assertStubBlock(...).\nPlease rewrite your code to place verifyThat in the assertStubBlock(...).";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Use of verifyThat must be inside of assertStubBlock:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertStubBlock()")
                )
        );
    }
}
