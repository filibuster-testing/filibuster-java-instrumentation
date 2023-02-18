package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterAnalysisFailureException;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.RedundantRPCWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RedundantRPCAnalyzer extends TestExecutionReportAnalyzer {
    public RedundantRPCAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    private final List<String> seenRPCs = new ArrayList<>();

    @Override
    void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject response) {
        String deiKey = distributedExecutionIndex.projectionLastKeyWithOnlySignature();
        JSONObject invocationArgsObject = invocation.getJSONObject("args");
        String invocationArgs = invocationArgsObject.getString("toString");
        String responseToEncode = "";

        // Could be null if the finish invocation didn't complete for some reason
        // i.e., test terminated and Filibuster shutdown before thread had a chance to run, I think?
        // We see this with coroutine usage, it could be a cancellation called after test completes and Filibuster shuts down.
        if (response != null) {
            if (response.has("return_value")) {
                responseToEncode = response.getJSONObject("return_value").toString();
            } else if (response.has("exception")) {
                responseToEncode = response.getJSONObject("exception").toString();
            } else {
                throw new FilibusterAnalysisFailureException("Response did not contain either a return value or an exception.");
            }

            String key = deiKey + invocationArgs + responseToEncode;
            String method = invocation.getString("method");

            if (seenRPCs.contains(key)) {
                this.addWarning(new RedundantRPCWarning(distributedExecutionIndex, method));
            } else {
                seenRPCs.add(key);
            }
        }
    }

    @Override
    boolean shouldReportErrorBasedOnTestStatus(boolean testPassed) {
        return true;
    }
}
