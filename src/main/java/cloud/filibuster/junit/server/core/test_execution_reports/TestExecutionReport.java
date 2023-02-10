package cloud.filibuster.junit.server.core.test_execution_reports;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class TestExecutionReport {
    private static final Logger logger = Logger.getLogger(TestExecutionReport.class.getName());

    private boolean hasReportBeenMaterialized = false;

    @Nullable
    private MaterializedReportMetadata materializedReportMetadata;

    private int testExecutionNumber = 0;

    private boolean testExecutionPassed = false;

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
        private static final String ITERATION_KEY = "iteration";
        private static final String STATUS_KEY = "status";
        private static final String DEI_KEY = "dei";
        private static final String REQUEST_KEY = "request";
        private static final String RESPONSE_KEY = "response";
        private static final String FAULT_KEY = "fault";
        private static final String RPCS_KEY = "rpcs";
    }

    @SuppressWarnings("MemberName")
    private JSONObject toJSONObject() {
        ArrayList<JSONObject> RPCs = new ArrayList<>();

        for (DistributedExecutionIndex dei : deiInvocationOrder) {
            JSONObject RPC = new JSONObject();
            RPC.put(Keys.DEI_KEY, dei.toString());
            RPC.put(Keys.REQUEST_KEY, deiInvocations.getOrDefault(dei, new JSONObject()));
            RPC.put(Keys.RESPONSE_KEY, deiResponses.getOrDefault(dei, new JSONObject()));
            RPC.put(Keys.FAULT_KEY, deiFaultsInjected.getOrDefault(dei, new JSONObject()));
            RPCs.add(RPC);
        }

        JSONObject result = new JSONObject();
        result.put(Keys.ITERATION_KEY, testExecutionNumber);
        result.put(Keys.STATUS_KEY, testExecutionPassed);
        result.put(Keys.RPCS_KEY, RPCs);
        return result;
    }

    private String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var analysis = " + jsonObject.toString(4) + ";";
    }

    public void writeTestReport(int currentIteration, boolean exceptionOccurred) {
        testExecutionNumber = currentIteration;
        testExecutionPassed = !exceptionOccurred;

        if (!hasReportBeenMaterialized) {
            try {
                // Create new directory for analysis report.
                UUID uuid = UUID.randomUUID();
                Path directory = Paths.get("/tmp/filibuster/filibuster-test-execution-" + uuid.toString());
                Files.createDirectory(directory);

                // Write out the actual JSON data.
                Path scriptFile = Files.createFile(Paths.get(directory.toString() + "/analysis.js"));
                Files.write(scriptFile, toJavascript().getBytes(Charset.defaultCharset()));

                // Write out index file.
                Path indexPath = Paths.get(directory + "/index.html");
                Files.write(indexPath, htmlContent.getBytes(Charset.defaultCharset()));

                // Set materialized and it's location.
                hasReportBeenMaterialized = true;
                materializedReportMetadata = new MaterializedReportMetadata(testExecutionNumber, testExecutionPassed, indexPath);

                logger.info(
                        "" + "\n" +
                                "[FILIBUSTER-CORE]: Test Execution Report written to file://" + indexPath + "\n");
                logger.info(
                        "" + "\n" +
                                "[FILIBUSTER-CORE]: Click me for tool view: http://filibuster.local" + indexPath + "\n");
            } catch (IOException e) {
                throw new FilibusterTestReportWriterException("Filibuster failed to write out the test execution report: ", e);
            }
        }
    }

    public MaterializedReportMetadata getMaterializedReportMetadata() {
        return this.materializedReportMetadata;
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
            "            <th>Distributed Execution Index</th>\n" +
            "            <th>RPC Method</th>\n" +
            "            <th>RPC Arguments</th>\n" +
            "            <th>Response</th>\n" +
            "            <th>Fault?</th>\n" +
            "        </tr>\n" +
            "\n" +
            "        <script>\n" +
            "                function isEmpty(obj) {\n" +
            "                    return Object.keys(obj).length === 0;\n" +
            "                }\n" +
            "\n" +
            "                function containsExceptionKey(obj) {\n" +
            "                    return \"exception\" in obj;\n" +
            "                }\n" +
            "\n" +
            "\t\t\t\t$(document).ready(function () {\n" +
            "                    console.log(analysis);\n" +
            "                    var status = analysis.status;\n" +
            "                    var iteration = analysis.iteration;\n" +
            "                    if (status) {\n" +
            "                        $('#status').html(\"Test Execution \" + iteration + \" <div class='pass'>Test Passed</div>\");\n" +
            "                    } else {\n" +
            "                        $('#status').html(\"Test Execution \" + iteration + \" <div class='fail'>Test Failed</div>\");\n" +
            "                    }\n" +
            "\n" +
            "                    for (i in analysis.rpcs) {\n" +
            "                        var rpc = analysis.rpcs[i];\n" +
            "                        var isFaulted = !isEmpty(rpc.fault);\n" +
            "                        var isExceptionResponse = containsExceptionKey(rpc.response);\n" +
            "                        console.log(rpc.response);\n" +
            "\n" +
            "                        var row = '';\n" +
            "\n" +
            "                        if (!isFaulted) {\n" +
            "                            if (isExceptionResponse) {\n" +
            "                                row += '<tr class=\"exception\">';\n" +
            "                            } else {\n" +
            "                                row += '<tr class=\"success\">';\n" +
            "                            }\n" +
            "                        } else {\n" +
            "                            row += '<tr class=\"fault\">';\n" +
            "                        }\n" +
            "\n" +
            "                        row += '<td class=\"dei\"><textarea>' + rpc.dei + '</textarea></td>';\n" +
            "                        row += '<td class=\"method\">' + rpc.request.method + '</td>';\n" +
            "                        row += '<td class=\"args\"><textarea>' + rpc.request.args + '</textarea></td>';\n" +
            "\n" +
            "                        if (isExceptionResponse) {\n" +
            "                            row += '<td>';\n" +
            "                            row += 'code = ' + rpc.response.exception.metadata.code + ', ';\n" +
            "                            if (rpc.response.exception.metadata.cause === \"\") {\n" +
            "                                row += 'cause = undefined';\n" +
            "                            } else {\n" +
            "                                row += 'cause = ' + rpc.response.exception.metadata.cause;\n" +
            "                            }\n" +
            "                            row += '</td>';\n" +
            "                        } else {\n" +
            "                            row += '<td class=\"response\"><textarea>' + rpc.response.return_value.toString + '</textarea></td>';\n" +
            "                        }\n" +
            "\n" +
            "                        if (!isFaulted) {\n" +
            "                            row += '<td></td>';\n" +
            "                        } else {\n" +
            "                            row += '<td>';\n" +
            "                            row += 'code = ' + rpc.fault.forced_exception.metadata.code + ', ';\n" +
            "                            if (rpc.fault.forced_exception.metadata.cause === \"\") {\n" +
            "                                row += 'cause = null';\n" +
            "                            } else {\n" +
            "                                row += 'cause = ' + rpc.fault.forced_exception.metadata.cause;\n" +
            "                            }\n" +
            "                            row += '</td>';\n" +
            "                        }\n" +
            "\n" +
            "                        row += '</tr>';\n" +
            "\t\t\t\t\t\t$('#table').append(row);\n" +
            "                    }\n" +
            "\t\t\t\t});\n" +
            "\t\t\t</script>\n" +
            "</section>\n" +
            "</body>\n" +
            "\n" +
            "</html>\n";
}
