package cloud.filibuster.junit.server.latency;

public class Filibuster1000msLatencyProfile implements FilibusterLatencyProfile {
    @Override
    public int getMsLatencyForMethod(String methodName) {
        return 1000;
    }

    @Override
    public int getMsLatencyForService(String serviceName) {
        return 0;
    }
}
