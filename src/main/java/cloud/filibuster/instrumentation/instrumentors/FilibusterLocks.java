package cloud.filibuster.instrumentation.instrumentors;

public class FilibusterLocks {
    public static final Object vectorClockLock = new Object();

    public static final Object distributedExecutionIndexLock = new Object();

    private FilibusterLocks() {

    }
}
