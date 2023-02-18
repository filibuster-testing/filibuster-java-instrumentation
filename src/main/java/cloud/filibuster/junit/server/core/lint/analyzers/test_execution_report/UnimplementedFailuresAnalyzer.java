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
    void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject fault, JSONObject response) {
        if (response != null) {
            if (response.has("exception")) {
                JSONObject exception = response.getJSONObject("exception");
                if (exception.has("metadata")) {
                    JSONObject metadata = exception.getJSONObject("metadata");
                    if (metadata.has("code")) {
                        String code = metadata.getString("code");
                        if (code.equals("UNIMPLEMENTED")) {
                            String method = invocation.getString("method");

                            boolean injectedUnimplementedFault = false;

                            if (fault != null) {
                                if (fault.has("forced_exception")) {
                                    JSONObject forcedException = fault.getJSONObject("forced_exception");

                                    if (forcedException.has("metadata")) {
                                        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
                                        String faultCode = forcedExceptionMetadata.getString("code");
                                        if (faultCode.equals(code)) {
                                            injectedUnimplementedFault = true;
                                        }
                                    }
                                }
                            }

                            if (!injectedUnimplementedFault) {
                                this.addWarning(new UnimplementedFailuresWarning(distributedExecutionIndex, method));
                            }
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
