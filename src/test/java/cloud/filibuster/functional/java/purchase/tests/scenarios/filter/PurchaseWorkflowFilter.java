package cloud.filibuster.functional.java.purchase.tests.scenarios.filter;

import cloud.filibuster.junit.filters.FilibusterFaultInjectionFilter;

public class PurchaseWorkflowFilter implements FilibusterFaultInjectionFilter {

    @Override
    public boolean shouldInjectFault(String methodName) {
        return methodName.contains("NotifyDiscountApplied");
    }
}
