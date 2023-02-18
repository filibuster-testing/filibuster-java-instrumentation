package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.IncompleteRPCWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IncompleteRPCAnalyzer extends TestExecutionReportAnalyzer {
    public IncompleteRPCAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    private final List<String> seenRPCs = new ArrayList<>();

    @Override
    void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject response) {
        String method = invocation.getString("method");

        if (response == null) {
            this.addWarning(new IncompleteRPCWarning(distributedExecutionIndex, method));
        }
    }

    @Override
    boolean shouldReportErrorBasedOnTestStatus(boolean testPassed) {
        return true;
    }
}
