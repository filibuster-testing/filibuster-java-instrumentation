package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

public class FilibusterGrpcInvokedRPCUnimplementedException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcInvokedRPCUnimplementedException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcInvokedRPCUnimplementedException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Invoked RPCs was left UNIMPLEMENTED. Use stubFor to implement stub.";
    }
}
