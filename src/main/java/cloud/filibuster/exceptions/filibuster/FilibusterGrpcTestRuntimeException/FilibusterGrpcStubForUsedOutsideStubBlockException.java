package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcStubForUsedOutsideStubBlockException} is thrown when stubFor is used outside stubBlock(...). " +
 * Please rewrite code to place stubFor in the stubBlock(...).
 */
public class FilibusterGrpcStubForUsedOutsideStubBlockException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcStubForUsedOutsideStubBlockException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcStubForUsedOutsideStubBlockException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Use of stubFor not allowed outside of stubBlock(...).\nPlease rewrite your code to place stubFor in the stubBlock(...).";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Use of stubFor must be inside of stubBlock:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#stubBlock()")
                )
        );
    }
}
