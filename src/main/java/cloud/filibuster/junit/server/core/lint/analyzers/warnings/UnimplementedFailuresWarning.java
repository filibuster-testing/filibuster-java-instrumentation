package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

import cloud.filibuster.dei.DistributedExecutionIndex;

public class UnimplementedFailuresWarning extends FilibusterAnalyzerWarning {

    public UnimplementedFailuresWarning(DistributedExecutionIndex distributedExecutionIndex, String details) {
        super(distributedExecutionIndex, details);
    }

    @Override
    public String getName() {
        return "UnimplementedFailures";
    }

    @Override
    public String getDescription() {
        return "RPCs that return UNIMPLEMENTED responses because they were not stubbed.";
    }

    @Override
    public String getRecommendations() {
        return "Stubs should be written for these RPCs.";
    }

    @Override
    public String getImpact() {
        return "Tests that pass with UNIMPLEMENTED RPCs may indicate that error handling code is too permissive, allowing tests to pass when RPCs fail.";
    }
}
