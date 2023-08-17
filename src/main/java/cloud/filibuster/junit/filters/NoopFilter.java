package cloud.filibuster.junit.filters;

public class NoopFilter implements FilibusterFaultInjectionFilter {
    @Override
    public boolean shouldInjectFault(String methodName) {
        return true;
    }
}
