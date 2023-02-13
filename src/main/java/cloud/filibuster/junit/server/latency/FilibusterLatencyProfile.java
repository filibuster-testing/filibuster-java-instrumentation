package cloud.filibuster.junit.server.latency;

public interface FilibusterLatencyProfile {
    int getMsLatencyForMethod(String methodName);

    int getMsLatencyForService(String methodName);
}
