package cloud.filibuster.junit.server.core.test_execution_reports;

import org.json.JSONObject;

import java.nio.file.Path;
import java.util.UUID;

public class MaterializedReportMetadata {
    private final int testExecutionNumber;

    private final boolean testExecutionPassed;

    private final Path reportPath;

    private final UUID uuid;

    public MaterializedReportMetadata(int testExecutionNumber, boolean testExecutionPassed, Path reportPath, UUID uuid) {
        this.testExecutionNumber = testExecutionNumber;
        this.testExecutionPassed = testExecutionPassed;
        this.reportPath = reportPath;
        this.uuid = uuid;
    }

    static class Keys {
        private static final String ITERATION_KEY = "iteration";
        private static final String STATUS_KEY = "status";
        private static final String PATH_KEY = "path";
        private static final String UUID_KEY = "uuid";
    }

    public JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        result.put(Keys.ITERATION_KEY, uuid.toString());
        result.put(Keys.ITERATION_KEY, testExecutionNumber);
        result.put(Keys.STATUS_KEY, testExecutionPassed);
        result.put(Keys.PATH_KEY, reportPath.toString());
        return result;
    }
}
