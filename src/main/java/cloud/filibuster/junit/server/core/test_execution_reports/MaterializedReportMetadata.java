package cloud.filibuster.junit.server.core.test_execution_reports;

import org.json.JSONObject;

import java.nio.file.Path;

public class MaterializedReportMetadata {
    private final int testExecutionNumber;

    private final boolean testExecutionPassed;

    private final Path reportPath;

    public MaterializedReportMetadata(int testExecutionNumber, boolean testExecutionPassed, Path reportPath) {
        this.testExecutionNumber = testExecutionNumber;
        this.testExecutionPassed = testExecutionPassed;
        this.reportPath = reportPath;
    }

    public JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        result.put("iteration", testExecutionNumber);
        result.put("status", testExecutionPassed);
        result.put("path", reportPath.toString());
        return result;
    }
}
