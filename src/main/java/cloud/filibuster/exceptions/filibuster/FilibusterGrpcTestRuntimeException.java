package cloud.filibuster.exceptions.filibuster;

public class FilibusterGrpcTestRuntimeException extends FilibusterRuntimeException {
    public FilibusterGrpcTestRuntimeException(String error, String recommendation) {
        super(error + "\n\t * " + recommendation);
    }

    public FilibusterGrpcTestRuntimeException(String error, String expected, String actual, String recommendation) {
        super(error + "\n\t * " + recommendation + "\nexpected: " + expected + "\nactual: " + actual);
    }

    public FilibusterGrpcTestRuntimeException(String error, String recommendation, Throwable t) {
        super(error + "\n\t * " + recommendation + "\n" + t.getClass().getName() + ": " + t.getMessage());
    }
}
