package cloud.filibuster.junit.server.core.test_execution_reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.IOException;
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
            Files.write(indexPath, defaultHtmlContent.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        }
    }

    private static final String defaultHtmlContent = "<html lang=\"en\">\n" +
            "\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Filibuster Test Execution Report</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "    <div style=\"margin: 0 auto; width: 100px;\">\n" +
            "        Please wait...\n" +
            "    </div>\n" +
            "\n" +
            "</body>\n" +
            "</html>";

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
        Path scriptFile = Paths.get(directory + "/analysis.js");
        try {
            Files.write(scriptFile, toJavascript().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
        }

        // Write out index file.
        Path indexPath = Paths.get(directory + "/index.html");
        try {
            Files.write(indexPath, htmlContent.getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution aggregate report: ", e);
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
        return "var analysis = " + jsonObject.toString(4) + ";";
    }

    private static final String htmlContent = "<html lang=\"en\">\n" +
            "\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Filibuster Test Execution Report</title>\n" +
            "    <script src=\"https://code.jquery.com/jquery-3.5.1.js\"></script>\n" +
            "    <script type=\"text/javascript\" src=\"./analysis.js\"></script>\n" +
            "    <style>\n" +
            "\t\ttable {\n" +
            "\t\t\tmargin: 0 auto;\n" +
            "\t\t\tfont-size: large;\n" +
            "\t\t\tborder: 1px solid black;\n" +
            "            table-layout: fixed;\n" +
            "\t\t}\n" +
            "\n" +
            "\t\th1 {\n" +
            "\t\t\ttext-align: center;\n" +
            "\t\t\tcolor: #006600;\n" +
            "\t\t\tfont-size: xx-large;\n" +
            "\t\t\tfont-family: 'Gill Sans',\n" +
            "\t\t\t\t'Gill Sans MT', ' Calibri',\n" +
            "\t\t\t\t'Trebuchet MS', 'sans-serif';\n" +
            "\t\t}\n" +
            "\n" +
            "        div {\n" +
            "\t\t\ttext-align: center;\n" +
            "            margin: 0 auto;\n" +
            "            width: 500px;\n" +
            "            padding-bottom: 10px;\n" +
            "        }\n" +
            "\n" +
            "        .fail { \n" +
            "            font-weight: bold;\n" +
            "            color: red;\n" +
            "        }\n" +
            "\n" +
            "        .pass { \n" +
            "            font-weight: bold;\n" +
            "            color: green;\n" +
            "        }\n" +
            "\n" +
            "\t\ttd {\n" +
            "\t\t\tborder: 1px solid black;\n" +
            "\t\t}\n" +
            "\n" +
            "\t\tth,\n" +
            "\t\ttd {\n" +
            "\t\t\tfont-weight: bold;\n" +
            "\t\t\tborder: 1px solid black;\n" +
            "\t\t\tpadding: 10px;\n" +
            "\t\t\ttext-align: center;\n" +
            "\t\t}\n" +
            "\n" +
            "\t\ttd {\n" +
            "            font-weight: lighter;\n" +
            "\t\t}\n" +
            "\n" +
            "        tr.success {\n" +
            "            background-color: green;\n" +
            "        }\n" +
            "\n" +
            "        tr.exception {\n" +
            "            background-color: yellow;\n" +
            "        }\n" +
            "\n" +
            "        tr.fault {\n" +
            "            background-color: red;\n" +
            "        }\n" +
            "\t</style>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "<section>\n" +
            "    <h1>Filibuster Test Execution Report</h1>\n" +
            "\n" +
            "    <div id='status'></div>\n" +
            "\n" +
            "    <table id='table'>\n" +
            "        <tr>\n" +
            "            <th>Test Execution</th>\n" +
            "            <th>Status</th>\n" +
            "            <th>Link</th>\n" +
            "        </tr>\n" +
            "\n" +
            "        <script>\n" +
            "\t\t\t\t$(document).ready(function () {\n" +
            "                    console.log(analysis);\n" +
            "\n" +
            "                    for (i in analysis.reports) {\n" +
            "                        var row = '';\n" +
            "                        var report = analysis.reports[i];\n" +
            "                        var statusText = '';\n" +
            "\n" +
            "                        if (report.status == true) {\n" +
            "                            statusText = '<font style=\"color: green\">Passed</font>';\n" +
            "                        } else {\n" +
            "                            statusText = '<font style=\"color: red\">Failed</font>';\n" +
            "                        }\n" +
            "\n" +
            "                        row += '<tr>';\n" +
            "                        row += '<td>' + report.iteration + '</td>';\n" +
            "                        row += '<td>' + statusText + '</td>';\n" +
            "                        row += '<td><a href=\"' + report.path + '\">Link To Report</a></td>';\n" +
            "                        row += '</tr>';\n" +
            "\n" +
            "\t\t\t\t\t\t$('#table').append(row);\n" +
            "                    }\n" +
            "\t\t\t\t});\n" +
            "\t\t\t</script>\n" +
            "</section>\n" +
            "</body>\n" +
            "\n" +
            "</html>\n";
}
