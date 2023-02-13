package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.MultipleInvocationsForIndividualMutationsWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cloud.filibuster.junit.server.core.lint.analyzers.LCS.computeLCS;

public class MultipleInvocationsForIndividualMutationsAnalyzer extends TestExecutionReportAnalyzer {
    private final List<Map.Entry<Integer, Map.Entry<DistributedExecutionIndex, JSONObject>>> previousRpcInvocations = new ArrayList<>();

    private final static int threshold = 10;

    public MultipleInvocationsForIndividualMutationsAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    @Override
    void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject response) {
        for (Map.Entry<Integer, Map.Entry<DistributedExecutionIndex, JSONObject>> previousInvocation: previousRpcInvocations) {
            int previousResponseInvocationNumber = previousInvocation.getKey();
            DistributedExecutionIndex previousDistributedExecutionIndex = previousInvocation.getValue().getKey();
            JSONObject previousInvocationObject = previousInvocation.getValue().getValue();

            String lcs = computeLCS(invocation.getString("args"), previousInvocationObject.getString("args"));

            String previousRequestInvocationMethod = previousInvocationObject.getString("method");

            boolean lcsAboveThreshold = lcs.length() >= threshold;
            boolean previousInvocationDirectlyBeforeRPC = (previousResponseInvocationNumber + 1 == RPC);
            boolean sameMethodAsTarget = previousInvocationObject.getString("method").equals(invocation.getString("method"));
            boolean sameArguments = previousInvocationObject.getString("args").equals(invocation.getString("args"));

            if (lcsAboveThreshold && previousInvocationDirectlyBeforeRPC && sameMethodAsTarget && !sameArguments) {
                this.addWarning(new MultipleInvocationsForIndividualMutationsWarning(distributedExecutionIndex,
                        "The following string (" + lcs + ") was used in a request to " + previousRequestInvocationMethod + " and used again to the same method in this test execution."));
            }
        }

        previousRpcInvocations.add(Pair.of(RPC, Pair.of(distributedExecutionIndex, invocation)));
    }

    @Override
    boolean shouldReportErrorBasedOnTestStatus(boolean testPassed) {
        return true;
    }
}
