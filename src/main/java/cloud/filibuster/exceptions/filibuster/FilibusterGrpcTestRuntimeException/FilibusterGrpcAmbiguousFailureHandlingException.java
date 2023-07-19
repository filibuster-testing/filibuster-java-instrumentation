package cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;

/**
 * {@code FilibusterGrpcAmbiguousFailureHandlingException} indicates that the compositional verification failed due to ambiguous failure handling.
 * Each fault introduced has different impact.
 * To fix this exception, write an assertOnFaults(...) for this fault combination with appropriate assertions.
 */
public class FilibusterGrpcAmbiguousFailureHandlingException extends FilibusterGrpcTestRuntimeException {

    public FilibusterGrpcAmbiguousFailureHandlingException() {
        super(getErrorMessage());
    }

    public FilibusterGrpcAmbiguousFailureHandlingException(Throwable cause) {
        super(getErrorMessage(), cause);
    }

    private static String getErrorMessage() {
        return "Compositional verification failed due to ambiguous failure handling: each fault introduced has different impact. " +
                "\nPlease write an assertOnFaults(...) for this fault combination with appropriate assertions.";
    }

    @Override
    public String getFixMessage() {
        return "<ul>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/GrpcMock.html#stubFor(io.grpc.MethodDescriptor,ReqT,RespT)\">" +
                            "Stub a GRPC method with a given request providing a particular response." +
                        "</a>" +
                    "</li>" +
                    "<li>" +
                        "<a target=\"_blank\" href=\"https://filibuster-testing.github.io/filibuster-java-instrumentation/javadoc/cloud/filibuster/junit/statem/GrpcMock.html#verifyThat(io.grpc.MethodDescriptor,ReqT,int)\">" +
                            "Set an expectation that a stub will be invoked, with a given request, a particular number of times." +
                        "</a>" +
                    "</li>" +
                "</ul>";
    }
}
