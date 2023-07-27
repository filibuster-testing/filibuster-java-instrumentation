package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcAssertionUsedOutsideFailureBlockException} is thrown when an assertion is used outside failureBlock(...). " +
 * Please rewrite code to place this assertion in the failureBlock(...).
 */
public class FilibusterGrpcAssertionUsedOutsideFailureBlockException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertionUsedOutsideFailureBlockException(String assertionName) {
        super(getErrorMessage(assertionName));
    }

    public FilibusterGrpcAssertionUsedOutsideFailureBlockException(String assertionName, Throwable cause) {
        super(getErrorMessage(assertionName), cause);
    }

    private static String getErrorMessage(String assertionName) {
        return "Use of assertions " + assertionName + " not allowed outside of failureBlock(...).\nPlease rewrite your code to place this assertion in the failureBlock(...).";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Use of assertions is only allowed inside of the failureBlock(...):",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#failureBlock()")
                )
        );
    }
}
