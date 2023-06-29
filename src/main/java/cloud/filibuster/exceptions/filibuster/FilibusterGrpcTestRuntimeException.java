package cloud.filibuster.exceptions.filibuster;

public class FilibusterGrpcTestRuntimeException extends FilibusterRuntimeException {
    public FilibusterGrpcTestRuntimeException(String error, String recommendation) {
        super(error + "\n\t * " + recommendation);
    }
}
