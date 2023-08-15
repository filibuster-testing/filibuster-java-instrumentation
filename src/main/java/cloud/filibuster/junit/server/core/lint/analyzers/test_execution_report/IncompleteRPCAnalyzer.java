package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.IncompleteRPCWarning;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IncompleteRPCAnalyzer extends TestExecutionReportAnalyzer {
    public IncompleteRPCAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    private final List<String> seenRpcs = new ArrayList<>();

    @Override
    void rpc(boolean testPassed, int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject fault, JSONObject response) {
        String method = invocation.getString("method");

        if (response == null) {
            this.addWarning(new IncompleteRPCWarning(distributedExecutionIndex, method));
        }
    }
}
