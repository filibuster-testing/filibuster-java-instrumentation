package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class TestReport {
    private static final Logger logger = Logger.getLogger(TestReport.class.getName());
    private final ArrayList<TestExecutionReport> testExecutionReports = new ArrayList<>();
    private final UUID testUUID;
    private final String testName;
    private final String className;

    public TestReport(String testName, UUID testUUID, String className) {
        this.testUUID = testUUID;
        this.testName = testName;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public UUID getTestUUID() {
        return testUUID;
    }

    public String getTestName() {
        return testName;
    }

    private int iterationsRemaining = 0;

    public void setIterationsRemaining(int iterationsRemaining) {
        this.iterationsRemaining = iterationsRemaining;
    }

    public void addTestExecutionReport(TestExecutionReport testExecutionReport) {
        testExecutionReports.add(testExecutionReport);
    }

    private File getDirectoryPath() {
        return new File(ReportUtilities.getBaseDirectoryPath(), "filibuster-test-" + testUUID.toString());
    }

    public File getReportPath() {
        File directory = getDirectoryPath();
        return new File(directory, "index.html");
    }

    public void writeOutPlaceholder() {
        File indexPath = getReportPath();
        try {
            //noinspection ResultOfMethodCallIgnored
            indexPath.getParentFile().mkdirs();
        } catch (SecurityException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test aggregate report: ", e);
        }

        try {
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_report/index.html");
            Files.write(indexPath.toPath(), indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test aggregate report: ", e);
        }
    }

    public void writeTestReport() {
        File directory = getDirectoryPath();

        try {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        } catch (SecurityException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        // Write out the actual JSON data.
        Path scriptFile = Paths.get(directory + "/summary.js");
        try {
            Files.write(scriptFile, toJavascript().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        // Write out index file.
        Path indexPath = Paths.get(directory + "/index.html");
        try {
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_report/index.html");
            Files.write(indexPath, indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        // Write out sidebar file.
        Path sidebarPath = Paths.get(directory + "/sidebar.html");
        try {
            byte[] sidebarBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_report/sidebar.html");
            Files.write(sidebarPath, sidebarBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        logger.info(
                "" + "\n" +
                        "[FILIBUSTER-CORE]: Test Execution Aggregate Report written to file://" + indexPath + "\n");
    }

    private JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        ArrayList<JSONObject> materializedReportMetadatas = new ArrayList<>();

        for (TestExecutionReport ter : testExecutionReports) {
            MaterializedTestExecutionReportMetadata mrm = ter.getMaterializedReportMetadata();

            if (mrm != null) {
                materializedReportMetadatas.add(ter.getMaterializedReportMetadata().toJSONObject());
            }
        }

        result.put("reports", materializedReportMetadatas);
        result.put("iterations_remaining", iterationsRemaining);
        return result;
    }

    private String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var summary = " + jsonObject.toString(4) + ";";
    }

    public ArrayList<TestExecutionReport> getTestExecutionReports() {
        return testExecutionReports;
    }
}
