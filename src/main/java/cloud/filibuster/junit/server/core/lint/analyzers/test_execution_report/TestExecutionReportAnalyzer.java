package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class TestExecutionReportAnalyzer {
    private final TestExecutionReport testExecutionReport;

    private final List<FilibusterAnalyzerWarning> warnings = new ArrayList<>();

    public TestExecutionReportAnalyzer(TestExecutionReport testExecutionReport) {
        this.testExecutionReport = testExecutionReport;
    }

    abstract void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, JSONObject response);

    public List<FilibusterAnalyzerWarning> getWarnings() {
        return this.warnings;
    }

    void addWarning(FilibusterAnalyzerWarning warning) {
        warnings.add(warning);
    }

    abstract boolean shouldReportErrorBasedOnTestStatus(boolean testPassed);

    public List<FilibusterAnalyzerWarning> analyze() {
        int i = 0;

        for (Iterator it = testExecutionReport.getInvocationOrderIterator(); it.hasNext(); ) {
            DistributedExecutionIndex distributedExecutionIndex = (DistributedExecutionIndex) it.next();

            JSONObject invocationObject = testExecutionReport.getInvocationObject(distributedExecutionIndex);
            JSONObject responseObject = testExecutionReport.getResponseObject(distributedExecutionIndex);

            rpc(i, distributedExecutionIndex, invocationObject, responseObject);

            i++;
        }

        return getWarnings();
    }
}
