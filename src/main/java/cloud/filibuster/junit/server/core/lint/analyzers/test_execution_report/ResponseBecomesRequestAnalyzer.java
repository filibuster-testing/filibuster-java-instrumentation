package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.ResponseBecomesRequestWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cloud.filibuster.junit.server.core.lint.analyzers.LCS.computeLCS;

public class ResponseBecomesRequestAnalyzer extends TestExecutionReportAnalyzer {
    public ResponseBecomesRequestAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    private final List<Map.Entry<Integer, Map.Entry<JSONObject, JSONObject>>> rpcResponses = new ArrayList<>();

    private final static int threshold = 10;

    @Override
    void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject response) {
        for (Map.Entry<Integer, Map.Entry<JSONObject, JSONObject>> previousResponse : rpcResponses) {
            int previousResponseInvocationNumber = previousResponse.getKey();
            String invocationArgs = invocation.getString("args");
            JSONObject previousResponseInvocation = previousResponse.getValue().getKey();
            JSONObject previousResponseObject = previousResponse.getValue().getValue();
            String previousResponseInvocationMethod = previousResponseInvocation.getString("method");
            String currentRequestInvocationMethod = invocation.getString("method");

            if (previousResponseObject.has("return_value")) {
                JSONObject previousResponseObjectReturnValue = previousResponseObject.getJSONObject("return_value");
                String lcs = computeLCS(invocationArgs, previousResponseObjectReturnValue.toString());

                boolean lcsAboveThreshold = lcs.length() >= threshold;
                boolean previousInvocationDirectlyBeforeRPC = (previousResponseInvocationNumber + 1) == RPC;
                boolean sameServiceAsTarget = previousResponseInvocation.getString("module").equals(invocation.getString("module"));

                if (lcsAboveThreshold && previousInvocationDirectlyBeforeRPC && sameServiceAsTarget) {
                    this.addWarning(new ResponseBecomesRequestWarning(distributedExecutionIndex, "The following string (" + lcs + ") used in a request to " + currentRequestInvocationMethod + " was found in a previous response from " + previousResponseInvocationMethod));
                }
            }
        }

        rpcResponses.add(Pair.of(RPC, Pair.of(invocation, response)));
    }

    @Override
    boolean shouldReportErrorBasedOnTestStatus(boolean testPassed) {
        return true;
    }
}
