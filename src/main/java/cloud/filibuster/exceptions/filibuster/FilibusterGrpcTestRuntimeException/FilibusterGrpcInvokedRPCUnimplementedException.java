package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcInvokedRPCUnimplementedException} is thrown when an invoked RPCs was left UNIMPLEMENTED.
 * Use stubFor to implement stub.
 */
public final class FilibusterGrpcInvokedRPCUnimplementedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcInvokedRPCUnimplementedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcInvokedRPCUnimplementedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Invoked RPCs was left UNIMPLEMENTED. Use stubFor to implement stub.";
    }

    @Override
    public String getFixMessage() {
        return null;
    }
}
