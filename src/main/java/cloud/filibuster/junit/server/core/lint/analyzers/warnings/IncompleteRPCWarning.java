package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

import cloud.filibuster.dei.DistributedExecutionIndex;

public class IncompleteRPCWarning extends FilibusterAnalyzerWarning {
    public IncompleteRPCWarning(DistributedExecutionIndex distributedExecutionIndex, String extendedWarning) {
        super(distributedExecutionIndex, extendedWarning);
    }

    @Override
    public String getName() {
        return "IncompleteRPC";
    }

    @Override
    public String getDescription() {
        return "Response was returned to the caller (or specifically, the test completed) before this RPC completed and logged its output.";
    }

    @Override
    public String getRecommendations() {
        return "Consider waiting for all RPCs issued concurrently, on different threads, to complete before returning response to caller.";
    }

    @Override
    public String getImpact() {
        return "Concurrent RPCs that are not cancelled and allowed to execute after the caller receives a response may reduce system performance.";
    }
}
