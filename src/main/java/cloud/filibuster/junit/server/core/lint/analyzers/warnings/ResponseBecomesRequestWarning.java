package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

import cloud.filibuster.dei.DistributedExecutionIndex;

public class ResponseBecomesRequestWarning extends FilibusterAnalyzerWarning {
    public ResponseBecomesRequestWarning(DistributedExecutionIndex distributedExecutionIndex, String extendedWarning) {
        super(distributedExecutionIndex, extendedWarning);
    }

    @Override
    public String getName() {
        return "ResponseBecomesRequest";
    }

    @Override
    public String getDescription() {
        return "Response from RPC becomes part of the request to an immediately subsequent RPC to the same service.";
    }

    @Override
    public String getRecommendations() {
        return "Consider altering the target RPC method to provide the desired response.";
    }

    @Override
    public String getImpact() {
        return "Executing more RPCs than required may both increase the brittleness of the existing code and slows down execution.";
    }
}
