package cloud.filibuster.exceptions.distributed_execution_index;

public class DistributedExecutionIndexSerializationException extends RuntimeException {
    public DistributedExecutionIndexSerializationException(String message) {
        super(message);
    }

    public DistributedExecutionIndexSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistributedExecutionIndexSerializationException(Throwable cause) {
        super(cause);
    }
}
