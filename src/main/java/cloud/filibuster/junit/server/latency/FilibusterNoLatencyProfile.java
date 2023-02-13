package cloud.filibuster.junit.server.latency;

public class FilibusterNoLatencyProfile implements FilibusterLatencyProfile {
    @Override
    public int getMsLatencyForMethod(String methodName) {
        return 0;
    }

    @Override
    public int getMsLatencyForService(String serviceName) {
        return 0;
    }
}
