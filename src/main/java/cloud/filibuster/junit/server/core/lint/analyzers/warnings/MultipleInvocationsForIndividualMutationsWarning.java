package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

import cloud.filibuster.dei.DistributedExecutionIndex;

public class MultipleInvocationsForIndividualMutationsWarning extends FilibusterAnalyzerWarning {
    public MultipleInvocationsForIndividualMutationsWarning(DistributedExecutionIndex distributedExecutionIndex, String extendedWarning) {
        super(distributedExecutionIndex, extendedWarning);
    }

    @Override
    public String getName() {
        return "MultipleInvocationsForIndividualMutations";
    }

    @Override
    public String getDescription() {
        return "Repeated invocations to the same RPC method using common elements.";
    }

    @Override
    public String getRecommendations() {
        return "Consider reducing RPC invocations by providing an API that allows for multiple mutations in a single RPC.";
    }

    @Override
    public String getImpact() {
        return "Individual mutations may violate assumed atomic commitment (i.e., multiple mutations where individual mutations may fail under assumption all will succeed.)  Additionally, multiple invocations are more expensive than a single invocation.";
    }
}
