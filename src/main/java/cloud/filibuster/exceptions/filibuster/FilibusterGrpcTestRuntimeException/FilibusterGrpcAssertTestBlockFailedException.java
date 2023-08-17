package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

import java.util.Arrays;

/**
 * {@code FilibusterGrpcAssertTestBlockFailedException} is invoked when assertions in assertTestBlock fail.
 * Please adjust assertions in assertTestBlock so that test passes.
 */
public class FilibusterGrpcAssertTestBlockFailedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertTestBlockFailedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAssertTestBlockFailedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertTestBlock failed.\nPlease adjust assertions in assertTestBlock so that test passes.";
    }

    @Override
    public String getFixMessage() {
        return generateFixMessage(
                Arrays.asList(
                        generateSingleFixMessage(
                                "Adjust test assertions:",
                                "https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/FilibusterGrpcTest.html#assertTestBlock()"
                        )
                )
        );
    }
}
