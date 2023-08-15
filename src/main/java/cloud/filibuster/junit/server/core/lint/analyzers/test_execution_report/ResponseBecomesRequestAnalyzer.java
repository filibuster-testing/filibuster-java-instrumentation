package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.ResponseBecomesRequestWarning;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cloud.filibuster.junit.server.core.lint.analyzers.LCS.computeLcs;

public class ResponseBecomesRequestAnalyzer extends TestExecutionReportAnalyzer {
    public ResponseBecomesRequestAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    private final List<Map.Entry<Integer, Map.Entry<JSONObject, JSONObject>>> previousRpcs = new ArrayList<>();

    private final static int threshold = 10;

    @Override
    void rpc(boolean testPassed, int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject fault, JSONObject response) {
        for (Map.Entry<Integer, Map.Entry<JSONObject, JSONObject>> previousRpc : previousRpcs) {
            int previousResponseInvocationNumber = previousRpc.getKey();
            JSONObject invocationArgsObject = invocation.getJSONObject("args");
            String invocationArgs = invocationArgsObject.getString("toString");

            JSONObject previousRpcInvocation = previousRpc.getValue().getKey();
            String previousResponseInvocationMethod = previousRpcInvocation.getString("method");
            String currentRequestInvocationMethod = invocation.getString("method");

            JSONObject previousRpcResponseObject = previousRpc.getValue().getValue();
            if (previousRpcResponseObject != null) {
                if (previousRpcResponseObject.has("return_value")) {
                    JSONObject previousResponseObjectReturnValue = previousRpcResponseObject.getJSONObject("return_value");
                    String lcs = computeLcs(invocationArgs, previousResponseObjectReturnValue.toString());

                    boolean lcsAboveThreshold = lcs.length() >= threshold;
                    boolean previousInvocationDirectlyBeforeRpc = (previousResponseInvocationNumber + 1) == RPC;
                    boolean sameServiceAsTarget = previousRpcInvocation.getString("module").equals(invocation.getString("module"));

                    if (lcsAboveThreshold && previousInvocationDirectlyBeforeRpc && sameServiceAsTarget) {
                        this.addWarning(new ResponseBecomesRequestWarning(distributedExecutionIndex, "The following string (" + lcs + ") used in a request to " + currentRequestInvocationMethod + " was found in a previous response from " + previousResponseInvocationMethod));
                    }
                }
            }
        }

        previousRpcs.add(Pair.of(RPC, Pair.of(invocation, response)));
    }
}
