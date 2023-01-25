package cloud.filibuster.exceptions.distributed_execution_index;

public class DistributedExecutionIndexCloneException extends RuntimeException {
    public DistributedExecutionIndexCloneException(String message) {
        super(message);
    }

    public DistributedExecutionIndexCloneException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistributedExecutionIndexCloneException(Throwable cause) {
        super(cause);
    }
}
