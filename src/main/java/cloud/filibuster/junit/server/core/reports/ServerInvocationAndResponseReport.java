package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.core.profiles.ServiceProfile;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ServerInvocationAndResponseReport {

    private static void addGrpcMethod(String grpcMethod) {
        grpcMethods.add(grpcMethod);
        grpcMethodInvokedByTests.put(grpcMethod, 0);
        grpcMethodInvokedByFilibusterTests.put(grpcMethod, 0);
        grpcMethodInvokedByUniqueFilibusterTests.put(grpcMethod, 0);
    }

    private static void incrementGrpcMethodByTestInvocation(String grpcMethod) {
        int currentValue = grpcMethodInvokedByTests.get(grpcMethod);
        grpcMethodInvokedByTests.put(grpcMethod, currentValue + 1);
    }

    private static void incrementGrpcMethodByFilibusterTestInvocation(String grpcMethod) {
        int currentValue = grpcMethodInvokedByFilibusterTests.get(grpcMethod);
        grpcMethodInvokedByFilibusterTests.put(grpcMethod, currentValue + 1);
    }

    private static void incrementGrpcMethodByUniqueFilibusterTestInvocation(String grpcMethod) {
        int currentValue = grpcMethodInvokedByUniqueFilibusterTests.get(grpcMethod);
        grpcMethodInvokedByUniqueFilibusterTests.put(grpcMethod, currentValue + 1);
    }

    private static final List<Integer> seenCoreHashCodes = new ArrayList<>();

    private static final List<String> grpcMethodsWithWarnings = new ArrayList<>();

    private static final List<String> grpcMethods = new ArrayList<>();

    private static final TreeMap<String, Integer> grpcMethodInvokedByTests = new TreeMap<>();

    private static final TreeMap<String, Integer> grpcMethodInvokedByFilibusterTests = new TreeMap<>();

    private static final TreeMap<String, Integer> grpcMethodInvokedByUniqueFilibusterTests = new TreeMap<>();

    @SuppressWarnings("ConstantPatternCompile")
    public static void loadGrpcEndpoints(Class<?> c) {
        String serviceName;

        try {
            Field serviceNameField = c.getField("SERVICE_NAME");
            serviceName = (String) serviceNameField.get(null);

            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {
                Pattern pattern = Pattern.compile("get(.*)Method");
                Matcher matcher = pattern.matcher(method.getName());
                if (matcher.find()) {
                    String strippedMethodName = matcher.group(1);
                    String fullMethodName = serviceName + "/" + strippedMethodName;
                    addGrpcMethod(fullMethodName);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.warning("Cannot populate Filibuster server invocation report, cannot determine service name for class " + c.getName() + " : " + e);
        }
    }

    @SuppressWarnings("ConstantPatternCompile")
    public static void loadGrpcEndpoints(String packageName) {
        Set<Class> allClassesInNamespace = findAllClassesUsingClassLoader(packageName);
        Set<Class<?>> grpcClasses = new HashSet<>();

        for (Class<?> c : allClassesInNamespace) {
            if (c.getName().endsWith("Grpc")) {
                grpcClasses.add(c);
            }
        }

        for (Class<?> c : grpcClasses) {
            loadGrpcEndpoints(c);
        }
    }

    private ServerInvocationAndResponseReport() {

    }

    public static void clear() {
        serverInvocationAndResponses = new ArrayList<>();
        incompleteServerInvocationAndResponses = new HashMap<>();
    }

    private static final Logger logger = Logger.getLogger(ServerInvocationAndResponseReport.class.getName());

    private static List<ServerInvocationAndResponse> serverInvocationAndResponses = new ArrayList<>();

    private static HashMap<String, GeneratedMessageV3> incompleteServerInvocationAndResponses = new HashMap<>();

    public static void beginServerInvocation(String requestId, GeneratedMessageV3 message) {
        incompleteServerInvocationAndResponses.put(requestId, message);
    }

    public static void endServerInvocation(String requestId, String fullMethodName, Status status, GeneratedMessageV3 responseMessage) {
        GeneratedMessageV3 requestMessage = incompleteServerInvocationAndResponses.get(requestId);
        ServerInvocationAndResponse serverInvocationAndResponse = new ServerInvocationAndResponse(requestId, fullMethodName, requestMessage, status, responseMessage);
        serverInvocationAndResponses.add(serverInvocationAndResponse);

        if (! grpcMethods.contains(fullMethodName)) {
            addGrpcMethod(fullMethodName);
            grpcMethodsWithWarnings.add(fullMethodName);
        }

        // Track server invocations.
        if (FilibusterCore.hasCurrentInstance()) {
            // Use of @TestWithFilibuster with filibuster.enabled = TRUE.
            incrementGrpcMethodByFilibusterTestInvocation(fullMethodName);

            int filibusterCoreHashCode = FilibusterCore.getCurrentInstance().hashCode();

            if (!seenCoreHashCodes.contains(filibusterCoreHashCode)) {
                incrementGrpcMethodByUniqueFilibusterTestInvocation(fullMethodName);
                seenCoreHashCodes.add(FilibusterCore.getCurrentInstance().hashCode());
            }
        } else {
            // Use of @Test with filibuster.enabled = TRUE.
            incrementGrpcMethodByTestInvocation(fullMethodName);
        }
    }

    private static ServiceProfile toServiceProfile() {
        ServiceProfile serviceProfile = new ServiceProfile();

        for (ServerInvocationAndResponse sir : serverInvocationAndResponses) {
            serviceProfile.addToProfile(sir.getFullMethodName(), sir.getRequestMessage(), sir.getResponseStatus(), sir.getResponseMessage());
        }

        return serviceProfile;
    }

    public static void writeServiceProfile() {
        toServiceProfile().writeServiceProfile();
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
        byte[] indexBytes = ReportUtilities.getResourceAsBytes(ServerInvocationAndResponseReport.class.getClassLoader(),"html/server_invocation_report/index.html");
        try {
            Files.write(indexPath, indexBytes);
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out the server invocation report: ", e);
        }

        logger.info(
                "" + "\n" +
                        "[FILIBUSTER-CORE]: Server Invocation Reports written to file://" + indexPath + "\n");
    }

    static class Keys {
        public static final String RESULTS_KEY = "results";
    }

    private static JSONObject toServerInvocationReportJSONObject() {
        JSONObject result = new JSONObject();
        List<JSONObject> results = new ArrayList<>();

        for (ServerInvocationAndResponse sir : serverInvocationAndResponses) {
            results.add(sir.toJSONObject());
        }

        result.put(Keys.RESULTS_KEY, results);
        return result;
    }

    private static JSONObject toAccessedGrpcEndpointsJSONObject() {
        JSONObject jsonObject = new JSONObject();
        List<JSONObject> jsonObjects = new ArrayList<>();

        for (String methodName : grpcMethods) {
            JSONObject methodJsonObject = new JSONObject();
            methodJsonObject.put("method", methodName);
            methodJsonObject.put("tests", grpcMethodInvokedByTests.get(methodName));
            methodJsonObject.put("filibuster_tests", grpcMethodInvokedByUniqueFilibusterTests.get(methodName));
            methodJsonObject.put("filibuster_executions", grpcMethodInvokedByFilibusterTests.get(methodName));

            if (grpcMethodsWithWarnings.contains(methodName)) {
                methodJsonObject.put("warning", true);

            } else {
                methodJsonObject.put("warning", false);
            }

            logger.log(Level.SEVERE, methodJsonObject.toString(4));
            jsonObjects.add(methodJsonObject);
        }

        jsonObject.put("methods", jsonObjects);
        return jsonObject;
    }

    private static String toJavascript() {
        String output = "";
        output += "var serverInvocationReports = " + toServerInvocationReportJSONObject().toString(4) + ";\n";
        output += "var accessedGrpcEndpoints = " + toAccessedGrpcEndpointsJSONObject().toString(4) + ";\n";
        return output;
    }

    public static Set<Class> findAllClassesUsingClassLoader(String packageName) {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        return reader.lines()
                .filter(line -> line.endsWith(".class"))
                .map(line -> getClass(line, packageName))
                .collect(Collectors.toSet());
    }

    @Nullable
    private static Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "."
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }
}
