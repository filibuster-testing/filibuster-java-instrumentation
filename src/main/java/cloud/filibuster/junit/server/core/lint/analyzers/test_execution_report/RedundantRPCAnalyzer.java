package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterAnalysisFailureException;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.RedundantRPCWarning;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static cloud.filibuster.instrumentation.helpers.Property.getTestAvoidRedundantInjectionsProperty;

public class RedundantRPCAnalyzer extends TestExecutionReportAnalyzer {
    private final TestExecutionReport testExecutionReport;

    public RedundantRPCAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
        this.testExecutionReport = testExecutionReport;
    }

    private final List<String> seenRpcs = new ArrayList<>();

    @Override
    void rpc(boolean testPassed, int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject fault, JSONObject response) {
        String deiKey = distributedExecutionIndex.projectionLastKeyWithOnlyMetadataAndSignature();
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
            } else if (response.has("byzantine_fault")) {
                responseToEncode = response.getJSONObject("byzantine_fault").toString();
            } else if (response.has("transformer_fault")) {
                responseToEncode = response.getJSONObject("transformer_fault").toString();
            } else {
                throw new FilibusterAnalysisFailureException("Response did not contain either a return value or an exception.");
            }

            String key = deiKey + invocationArgs + responseToEncode;
            String method = invocation.getString("method");

            if (seenRpcs.contains(key)) {
                if (getTestAvoidRedundantInjectionsProperty()) {
                    List<DistributedExecutionIndex> cachedRpcs = testExecutionReport.getCachedRpcs();
                    if (!cachedRpcs.contains(distributedExecutionIndex)) {
                        this.addWarning(new RedundantRPCWarning(distributedExecutionIndex, method));
                    }
                } else {
                    this.addWarning(new RedundantRPCWarning(distributedExecutionIndex, method));
                }
            } else {
                seenRpcs.add(key);
            }
        }
    }
}
