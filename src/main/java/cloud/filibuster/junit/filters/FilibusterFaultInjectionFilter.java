package cloud.filibuster.junit.filters;

public interface FilibusterFaultInjectionFilter {
    boolean shouldInjectFault(String methodName);
}
