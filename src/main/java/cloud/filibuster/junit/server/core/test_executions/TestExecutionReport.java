package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class TestExecutionReport {
    private static final Logger logger = Logger.getLogger(TestExecutionReport.class.getName());

    private final ArrayList<DistributedExecutionIndex> deiInvocationOrder = new ArrayList<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiInvocations = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiResponses = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiFaultsInjected = new HashMap<>();

    public void recordInvocation(
            DistributedExecutionIndex distributedExecutionIndex,
            JSONObject invocationJsonObject
    ) {
        // Add to invocation order list.
        deiInvocationOrder.add(distributedExecutionIndex);

        // ...then, record the information about the invocation.
        deiInvocations.put(distributedExecutionIndex, invocationJsonObject);
    }

    public void recordInvocationComplete(
            DistributedExecutionIndex distributedExecutionIndex,
            JSONObject invocationJsonObject
    ) {
        // Record the information about the invocation's response.
        deiResponses.put(distributedExecutionIndex, invocationJsonObject);
    }

    public void setFaultsInjected(HashMap<DistributedExecutionIndex, JSONObject> faultsToInject) {
        deiFaultsInjected.putAll(faultsToInject);
    }

    static class Keys {
        private static String DEI_KEY = "dei";
        private static String DETAILS_KEY = "details";
        private static String FAULT_KEY = "fault";
        private static String RPCS_KEY = "rpcs";
        private static String PASSED_KEY = "passed";
    }

    @SuppressWarnings("MemberName")
    public JSONObject toJSONObject() {
        ArrayList<JSONObject> RPCs = new ArrayList<>();

        for (DistributedExecutionIndex dei : deiInvocationOrder) {
            JSONObject RPC = new JSONObject();
            RPC.put(Keys.DEI_KEY, dei.toString());
            RPC.put(Keys.DETAILS_KEY, deiInvocations.getOrDefault(dei, new JSONObject()));
            RPC.put(Keys.FAULT_KEY, deiFaultsInjected.getOrDefault(dei, new JSONObject()));
            RPCs.add(RPC);
        }

        JSONObject result = new JSONObject();
        result.put(Keys.RPCS_KEY, RPCs);
        return result;
    }

    public String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var analysis = " + jsonObject.toString(4) + ";";
    }

    public void writeTestReport() {
        try {

            // Create new directory for analysis report.
            Path directory = Files.createTempDirectory("filibuster-test-execution-");

            // Write out the actual JSON report.
            Path scriptFile = Files.createFile(Paths.get(directory.toString() + "/analysis.js"));
            Files.write(scriptFile, toJavascript().getBytes());

            // Copy index file over.
            Path indexPath = Paths.get(directory + "/index.html");
            Files.write(indexPath, htmlContent.getBytes());

            logger.info(
                    "" + "\n" +
                            "[FILIBUSTER-CORE]: Test Execution Report written to file://" + indexPath + "\n");
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException(e);
        }
    }

    private static final SecureRandom random = new SecureRandom();

    private static String generateFilename() {
        long n = random.nextLong();

        if (n == Long.MIN_VALUE) {
            n = 0;
        } else {
            n = Math.abs(n);
        }

        return "filibuster-test-execution-report-" + n;
    }

    private String htmlContent = "<html lang=\"en\">\n" +
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
            "    <table id='table'>\n" +
            "        <tr>\n" +
            "            <th>Distributed Execution Index</th>\n" +
            "            <th>RPC Method</th>\n" +
            "            <th>RPC Arguments</th>\n" +
            "            <th>Fault</th>\n" +
            "        </tr>\n" +
            "\n" +
            "        <script>\n" +
            "                function isEmpty(obj) {\n" +
            "                    return Object.keys(obj).length === 0;\n" +
            "                }\n" +
            "\n" +
            "\t\t\t\t$(document).ready(function () {\n" +
            "                    for (i in analysis.rpcs) {\n" +
            "                        var rpc = analysis.rpcs[i];\n" +
            "                        var isFaulted = !isEmpty(rpc.fault);\n" +
            "                        console.log(rpc);\n" +
            "                        console.log(rpc.fault);\n" +
            "                        console.log(isEmpty(rpc.fault));\n" +
            "\n" +
            "                        var row = '';\n" +
            "\n" +
            "                        if (!isFaulted) {\n" +
            "                            row += '<tr class=\"success\">';\n" +
            "                        } else {\n" +
            "                            row += '<tr class=\"fault\">';\n" +
            "                        }\n" +
            "\n" +
            "                        row += '<td class=\"dei\"><textarea>' + rpc.dei + '</textarea></td>';\n" +
            "                        row += '<td class=\"method\">' + rpc.details.method + '</td>';\n" +
            "                        row += '<td class=\"args\"><textarea>' + rpc.details.args + '</textarea></td>';\n" +
            "\n" +
            "                        if (!isFaulted) {\n" +
            "                            row += '<td></td>';\n" +
            "                        } else {\n" +
            "                            row += '<td>' + rpc.fault.forced_exception.metadata.code + '</td>';\n" +
            "                        }\n" +
            "\n" +
            "                        row += '</tr>';\n" +
            "\t\t\t\t\t\t$('#table').append(row);\n" +
            "\n" +
            "                    }\n" +
            "\t\t\t\t});\n" +
            "\t\t\t</script>\n" +
            "</section>\n" +
            "</body>\n" +
            "\n" +
            "</html>\n";
}
