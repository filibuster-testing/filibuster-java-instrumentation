package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

public class FilibusterGrpcAssertTestBlockFailedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAssertTestBlockFailedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAssertTestBlockFailedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Assertions in assertTestBlock failed. Please adjust assertions in assertTestBlock so that test passes.";
    }
}
