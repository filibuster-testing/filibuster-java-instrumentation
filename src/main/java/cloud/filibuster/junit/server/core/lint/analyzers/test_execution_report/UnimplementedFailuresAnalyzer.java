package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.UnimplementedFailuresWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

public class UnimplementedFailuresAnalyzer extends TestExecutionReportAnalyzer {
    public UnimplementedFailuresAnalyzer(TestExecutionReport testExecutionReport) {
        super(testExecutionReport);
    }

    @Override
    void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject response) {
        if (response != null) {
            if (response.has("exception")) {
                JSONObject exception = response.getJSONObject("exception");
                if (exception.has("metadata")) {
                    JSONObject metadata = exception.getJSONObject("metadata");
                    if (metadata.has("code")) {
                        String code = metadata.getString("code");
                        if (code.equals("UNIMPLEMENTED")) {
                            String method = invocation.getString("method");
                            this.addWarning(new UnimplementedFailuresWarning(distributedExecutionIndex, method));
                        }
                    }
                }
            }
        }
    }

    @Override
    boolean shouldReportErrorBasedOnTestStatus(boolean testPassed) {
        return testPassed;
    }
}
