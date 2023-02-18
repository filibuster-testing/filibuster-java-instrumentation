package cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterAnalysisFailureException;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionReport;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class TestExecutionReportAnalyzer {
    private final TestExecutionReport testExecutionReport;

    private final List<FilibusterAnalyzerWarning> warnings = new ArrayList<>();

    public TestExecutionReportAnalyzer(TestExecutionReport testExecutionReport) {
        this.testExecutionReport = testExecutionReport;
    }

    abstract void rpc(int RPC, DistributedExecutionIndex distributedExecutionIndex, JSONObject invocation, @Nullable JSONObject fault, @Nullable JSONObject response);

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
            JSONObject faultObject = testExecutionReport.getFaultObject(distributedExecutionIndex);
            JSONObject responseObject = testExecutionReport.getResponseObject(distributedExecutionIndex);

            try {
                rpc(i, distributedExecutionIndex, invocationObject, faultObject, responseObject);
            } catch (RuntimeException e) {
                throw new FilibusterAnalysisFailureException("Analyzer " + this.getClass() + " failed with exception: " + e);
            }

            i++;
        }

        return getWarnings();
    }
}
