package cloud.filibuster.junit.server.core.test_execution_reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

public class TestExecutionAggregateReport {
    private static final Logger logger = Logger.getLogger(TestExecutionAggregateReport.class.getName());

    private final ArrayList<TestExecutionReport> testExecutionReports = new ArrayList<>();

    public void addTestExecutionReport(TestExecutionReport testExecutionReport) {
        testExecutionReports.add(testExecutionReport);
    }

    public void writeOutPlaceholder() {
        Path directory = Paths.get("/tmp/filibuster");
        Path indexPath = Paths.get(directory + "/index.html");

        try {
            Files.createDirectory(directory);
        } catch(FileAlreadyExistsException e) {
            // ignored.
        } catch(IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        }

        try {
            Path constructionGifPath = Paths.get(directory + "/construction.gif");
            File constructionGifResourcePath = getFileFromResource("html/test_execution_aggregate_report/construction.gif");
            Files.write(constructionGifPath, Files.readAllBytes(constructionGifResourcePath.toPath()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        } catch (URISyntaxException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file: ", e);
        }

        try {
            File resourceIndexPath = getFileFromResource("html/test_execution_aggregate_report/waiting.html");
            Files.write(indexPath, Files.readAllBytes(resourceIndexPath.toPath()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        } catch (URISyntaxException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file: ", e);
        }
    }

    private File getFileFromResource(String fileName) throws URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);

        if (resource == null) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file; this is possibly a file not found for file: " + fileName);
        } else {
            return new File(resource.toURI());
        }
    }

    public void writeTestExecutionAggregateReport() {
        Path directory = Paths.get("/tmp/filibuster/");

        try {
            Files.createDirectory(directory);
        } catch(FileAlreadyExistsException e) {
            // Ignore.
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        }

        // Write out the actual JSON data.
        Path scriptFile = Paths.get(directory + "/summary.js");
        try {
            Files.write(scriptFile, toJavascript().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        }

        // Write out index file.
        Path indexPath = Paths.get(directory + "/index.html");
        try {
            File resourceIndexPath = getFileFromResource("html/test_execution_aggregate_report/index.html");
            Files.write(indexPath, Files.readAllBytes(resourceIndexPath.toPath()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        } catch (URISyntaxException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file: ", e);
        }

        logger.info(
                "" + "\n" +
                        "[FILIBUSTER-CORE]: Test Execution Report written to file://" + indexPath + "\n");
    }

    private JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        ArrayList<JSONObject> materializedReportMetadatas = new ArrayList<>();

        for (TestExecutionReport ter : testExecutionReports) {
            MaterializedReportMetadata mrm = ter.getMaterializedReportMetadata();

            if (mrm != null) {
                materializedReportMetadatas.add(ter.getMaterializedReportMetadata().toJSONObject());
            }
        }

        result.put("reports", materializedReportMetadatas);
        return result;
    }

    private String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var summary = " + jsonObject.toString(4) + ";";
    }
}
