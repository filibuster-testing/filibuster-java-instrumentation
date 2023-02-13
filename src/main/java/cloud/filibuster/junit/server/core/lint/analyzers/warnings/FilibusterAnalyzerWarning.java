package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

import cloud.filibuster.dei.DistributedExecutionIndex;

public abstract class FilibusterAnalyzerWarning {
    private final DistributedExecutionIndex distributedExecutionIndex;

    private String details;

    public FilibusterAnalyzerWarning(DistributedExecutionIndex distributedExecutionIndex, String details) {
        this.distributedExecutionIndex = distributedExecutionIndex;
        this.details = details;
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getRecommendations();

    public abstract String getImpact();

    public String getDetails() {
        return this.details;
    }

    public DistributedExecutionIndex getDistributedExecutionIndex() {
        return this.distributedExecutionIndex;
    }
}
