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
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/GrpcMock.html#stubFor(io.grpc.MethodDescriptor,ReqT,RespT)\">" +
                            "Stub a GRPC method with a given request providing a particular response." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
