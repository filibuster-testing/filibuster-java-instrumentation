package cloud.filibuster.junit.server.core.invocations;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class ServerInvocationAndResponseReport {
    private ServerInvocationAndResponseReport() {

    }

    private static final Logger logger = Logger.getLogger(ServerInvocationAndResponseReport.class.getName());

    private static final List<ServerInvocationAndResponse> serverInvocationAndResponses = new ArrayList<>();

    private static final HashMap<String, GeneratedMessageV3> incompleteServerInvocationAndResponses = new HashMap<>();

    public static List<ServerInvocationAndResponse> getServerInvocationAndResponses() {
        return serverInvocationAndResponses;
    }

    public static void beginServerInvocation(String requestId, GeneratedMessageV3 message) {
        incompleteServerInvocationAndResponses.put(requestId, message);
    }

    public static void endServerInvocation(String requestId, String fullMethodName, Status status, GeneratedMessageV3 responseMessage) {
        GeneratedMessageV3 requestMessage = incompleteServerInvocationAndResponses.get(requestId);
        ServerInvocationAndResponse serverInvocationAndResponse = new ServerInvocationAndResponse(requestId, fullMethodName, requestMessage, status, responseMessage);
        serverInvocationAndResponses.add(serverInvocationAndResponse);
    }

    public static void writeServerInvocationReport() {
        Path reportDirectory = Paths.get("/tmp/filibuster/");

        try {
            Files.createDirectory(reportDirectory);
        } catch(FileAlreadyExistsException e) {
            // Ignore.
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the server invocation report: ", e);
        }

        // Write out the actual Javascript data.
        Path scriptFile = Paths.get(reportDirectory + "/server.js");
        try {
            Files.write(scriptFile, toJavascript().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the server invocation report: ", e);
        }

        // Write out index file.
        Path indexPath = Paths.get(reportDirectory + "/server.html");
        byte[] indexBytes = getResourceAsBytes("html/server_invocation_report/index.html");
        try {
            Files.write(indexPath, indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the server invocation report: ", e);
        }

        logger.info(
                "" + "\n" +
                        "[FILIBUSTER-CORE]: Server Invocation Reports written to file://" + indexPath + "\n");

        Path serverDirectory = Paths.get("/tmp/filibuster/server");

        try {
            Files.createDirectory(serverDirectory);
        } catch(FileAlreadyExistsException e) {
            // Ignore.
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to create server report directory ", e);
        }

        // Write out the actual JSON data.
        Path jsonFile = Paths.get(serverDirectory + "/server.json");
        try {
            Files.write(jsonFile, toJSONObject().toString().getBytes(Charset.defaultCharset()));
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the server report to report directory: ", e);
        }

    }

    static class Keys {
        public static final String RESULTS_KEY = "results";
    }

    private static JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        List<JSONObject> results = new ArrayList<>();

        for (ServerInvocationAndResponse sir : serverInvocationAndResponses) {
            results.add(sir.toJSONObject());
        }

        result.put(Keys.RESULTS_KEY, results);
        return result;
    }

    private static String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var serverInvocationReports = " + jsonObject.toString(4) + ";";
    }

    private static byte[] getResourceAsBytes(String fileName) {
        ClassLoader classLoader = ServerInvocationAndResponseReport.class.getClassLoader();
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
}
