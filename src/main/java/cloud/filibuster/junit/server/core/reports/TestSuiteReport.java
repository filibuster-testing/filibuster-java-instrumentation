package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestSuiteReport {

    static class Keys {
        private static final String REPORTS_KEY = "reports";

        static class TestReportKeys {
            private static final String STATUS = "status";
            private static final String TEST_PATH = "path";
            private static final String TEST_NAME = "test_name";
        }

    }

    @SuppressWarnings("InnerClassMayBeStatic")
    private class FilibusterTestReportSummary {
        private final String testName;
        private final File testPath;
        private final boolean status;

        public FilibusterTestReportSummary(String testName, File testPath, boolean status) {
            this.testName = testName;
            this.testPath = testPath;
            this.status = status;
        }
    }

    private static TestSuiteReport instance;

    private static final Logger logger = Logger.getLogger(TestSuiteReport.class.getName());
    /**
     * A map where the keys are the display name of the tests and the value is a list of the UUIDs associated with each execution
     */
    private final ArrayList<FilibusterTestReportSummary> testReportSummaries = new ArrayList<>();

    public static TestSuiteReport getInstance() {
        if (instance == null) {
            instance = new TestSuiteReport();
        }
        return instance;
    }

    private TestSuiteReport() {
        Thread testSuiteCompleteHook = new Thread(this::testSuiteCompleted);
        Runtime.getRuntime().addShutdownHook(testSuiteCompleteHook);
        startTestSuite();
    }

    private void startTestSuite() {
        try (Stream<Path> filesInDirectoryStream = Files.walk(ReportUtilities.GetBaseDirectoryPath().toPath())) {
            //noinspection ResultOfMethodCallIgnored
            filesInDirectoryStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(file -> file.toString().contains("filibuster-test-"))
                    .forEach(File::delete);
        } catch (NoSuchFileException e) {
            //Ignore since it's not there
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to delete content in the /tmp/filibuster/ directory ", e);
        }

        writeOutPlaceholder();
    }

    private void testSuiteCompleted() {

        ServerInvocationAndResponseReport.writeServerInvocationReport();

        ServerInvocationAndResponseReport.writeServiceProfile();

        writeOutReports();
    }

    private JSONObject getTestReportSummaryJSON(FilibusterTestReportSummary testReportSummary) {
        JSONObject reportJSON = new JSONObject();
        reportJSON.put(Keys.TestReportKeys.TEST_PATH, testReportSummary.testPath);
        reportJSON.put(Keys.TestReportKeys.TEST_NAME, testReportSummary.testName);
        reportJSON.put(Keys.TestReportKeys.STATUS, testReportSummary.status);
        return reportJSON;
    }

    private JSONObject getReportsJSON() {
        JSONObject reportsJSON = new JSONObject();
        List<JSONObject> jsonReports = testReportSummaries.stream()
                .map(this::getTestReportSummaryJSON).collect(Collectors.toList());
        reportsJSON.put(Keys.REPORTS_KEY, jsonReports);
        return reportsJSON;
    }

    private void writeOutPlaceholder() {
        File directory = ReportUtilities.GetBaseDirectoryPath();
        File indexPath = new File(directory, "index.html");

        try {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        } catch (SecurityException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report placeholder: ", e);
        }

        try {
            Path constructionGifPath = Paths.get(directory + "/construction.gif");
            byte[] constructionGifBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_suite_report/construction.gif");
            Files.write(constructionGifPath, constructionGifBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report: ", e);
        }

        try {
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_suite_report/waiting.html");
            Files.write(indexPath.toPath(), indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report: ", e);
        }
    }

    private void writeOutReports() {
        File directory = ReportUtilities.GetBaseDirectoryPath();
        File scriptFile = new File(directory, "summary.js");
        try {
            Files.write(scriptFile.toPath(), ("var summary = " + getReportsJSON().toString(4) + ";")
                    .getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test suite report: ", e);
        }
        // Write out index file.
        Path indexPath = Paths.get(directory + "/index.html");
        try {
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_suite_report/index.html");
            Files.write(indexPath, indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        logger.info(
                "" + "\n" +
                        "[FILIBUSTER-CORE]: Test Suite Report written to file://" + indexPath + "\n");

    }


    public void addTestReport(TestReport testReport) {
        String testName = testReport.getTestName();
        File testPath = testReport.getReportPath();
        ArrayList<TestExecutionReport> testExecutionReports = testReport.getTestExecutionReports();
        boolean hasNoFailures = testExecutionReports.stream().map(TestExecutionReport::isTestExecutionPassed)
                .reduce(true, (curr, next) -> curr && next);
        testReportSummaries.add(new FilibusterTestReportSummary(testName, testPath, hasNoFailures));
    }

}
