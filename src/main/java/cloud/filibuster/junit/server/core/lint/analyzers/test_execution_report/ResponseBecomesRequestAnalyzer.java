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

    private final List<Map.Entry<Integer, Map.Entry<JSONObject, JSONObject>>> previousRPCs = new ArrayList<>();

    private final static int threshold = 10;

    @Override
    void rpc(boolean testPassed, int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject fault, JSONObject response) {
        for (Map.Entry<Integer, Map.Entry<JSONObject, JSONObject>> previousRPC : previousRPCs) {
            int previousResponseInvocationNumber = previousRPC.getKey();
            JSONObject invocationArgsObject = invocation.getJSONObject("args");
            String invocationArgs = invocationArgsObject.getString("toString");

            JSONObject previousRPCInvocation = previousRPC.getValue().getKey();
            String previousResponseInvocationMethod = previousRPCInvocation.getString("method");
            String currentRequestInvocationMethod = invocation.getString("method");

            JSONObject previousRPCResponseObject = previousRPC.getValue().getValue();
            if (previousRPCResponseObject != null) {
                if (previousRPCResponseObject.has("return_value")) {
                    JSONObject previousResponseObjectReturnValue = previousRPCResponseObject.getJSONObject("return_value");
                    String lcs = computeLcs(invocationArgs, previousResponseObjectReturnValue.toString());

                    boolean lcsAboveThreshold = lcs.length() >= threshold;
                    boolean previousInvocationDirectlyBeforeRPC = (previousResponseInvocationNumber + 1) == RPC;
                    boolean sameServiceAsTarget = previousRPCInvocation.getString("module").equals(invocation.getString("module"));

                    if (lcsAboveThreshold && previousInvocationDirectlyBeforeRPC && sameServiceAsTarget) {
                        this.addWarning(new ResponseBecomesRequestWarning(distributedExecutionIndex, "The following string (" + lcs + ") used in a request to " + currentRequestInvocationMethod + " was found in a previous response from " + previousResponseInvocationMethod));
                    }
                }
            }
        }

        previousRPCs.add(Pair.of(RPC, Pair.of(invocation, response)));
    }
}
