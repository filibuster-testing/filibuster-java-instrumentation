package cloud.filibuster.junit.filters;

public class WorldServiceFilter implements FilibusterFaultInjectionFilter {
    @Override
    public boolean shouldInjectFault(String methodName) {
        return !methodName.contains("WorldService");
    }
}
