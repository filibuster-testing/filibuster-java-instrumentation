package cloud.filibuster.junit.filters;

public class WorldServiceFilter implements FilibusterFaultInjectionFilter {
    public boolean shouldInjectFault(String methodName) {
        return !methodName.contains("WorldService");
    }
}
