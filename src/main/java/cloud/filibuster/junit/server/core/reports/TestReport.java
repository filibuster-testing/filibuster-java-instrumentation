package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class TestReport {
    private static final Logger logger = Logger.getLogger(TestReport.class.getName());

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
            // Nothing, directory already exists.
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        }

        try (Stream<Path> filesInDirectoryStream  =  Files.walk(directory) ){
            filesInDirectoryStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .filter(file -> file.toString().contains("filibuster-test-execution"))
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to delete content in the /tmp/filibuster/ directory ", e);
        }

        try {
            Path constructionGifPath = Paths.get(directory + "/construction.gif");
            byte[] constructionGifBytes = getResourceAsBytes("html/test_report/construction.gif");
            Files.write(constructionGifPath, constructionGifBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }

        try {
            byte[] indexBytes = getResourceAsBytes("html/test_report/waiting.html");
            Files.write(indexPath, indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
        }
    }

    private byte[] getResourceAsBytes(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream resource = classLoader.getResourceAsStream(fileName);

        if (resource == null) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file because it is null; this is possibly a file not found for file: " + fileName);
        }

        byte[] targetArray = new byte[0];

        try {
            targetArray = new byte[resource.available()];
            resource.read(targetArray);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to open resource file because of exception; this is possibly a file not found for file: " + fileName, e);
        }

        return targetArray;
    }

    public void writeTestReport() {
        Path directory = Paths.get("/tmp/filibuster/");

        try {
            Files.createDirectory(directory);
        } catch(FileAlreadyExistsException e) {
            // Ignore.
        } catch (IOException e) {
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
            byte[] indexBytes = getResourceAsBytes("html/test_report/index.html");
            Files.write(indexPath, indexBytes);
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
        return result;
    }

    private String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var summary = " + jsonObject.toString(4) + ";";
    }
}