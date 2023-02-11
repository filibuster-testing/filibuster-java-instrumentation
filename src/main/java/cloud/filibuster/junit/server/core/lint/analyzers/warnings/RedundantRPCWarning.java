package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

import cloud.filibuster.dei.DistributedExecutionIndex;

public class RedundantRPCWarning extends FilibusterAnalyzerWarning {

    public RedundantRPCWarning(DistributedExecutionIndex distributedExecutionIndex, String details) {
        super(distributedExecutionIndex, details);
    }

    @Override
    public String getName() {
        return "RedundantRPC";
    }

    @Override
    public String getDescription() {
        return "RPC is invoked repeatedly with the same arguments and returns the same response.";
    }

    @Override
    public String getRecommendations() {
        return "Reduce redundant RPCs by passing already retrieved arguments by value or apply in-memory caching to avoid execution of redundant RPCs.";
    }

    @Override
    public String getImpact() {
        return "Executing more RPCs than required may both increase the brittleness of the existing code and slows down execution.";
    }
}
