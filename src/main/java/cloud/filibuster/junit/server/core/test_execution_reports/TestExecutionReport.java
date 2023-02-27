package cloud.filibuster.junit.server.core.test_execution_reports;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterAnalysisFailureException;
import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.IncompleteRPCAnalyzer;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.MultipleInvocationsForIndividualMutationsAnalyzer;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.RedundantRPCAnalyzer;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.ResponseBecomesRequestAnalyzer;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.TestExecutionReportAnalyzer;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.UnimplementedFailuresAnalyzer;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private final List<FilibusterAnalyzerWarning> warnings = new ArrayList<>();

    private final UUID uuid = UUID.randomUUID();

    public List<FilibusterAnalyzerWarning> getWarnings() {
        return this.warnings;
    }

    public Iterator<DistributedExecutionIndex> getInvocationOrderIterator() {
        return deiInvocationOrder.iterator();
    }

    public JSONObject getInvocationObject(DistributedExecutionIndex distributedExecutionIndex) {
        return deiInvocations.get(distributedExecutionIndex);
    }

    public JSONObject getResponseObject(DistributedExecutionIndex distributedExecutionIndex) {
        return deiResponses.get(distributedExecutionIndex);
    }

    public JSONObject getFaultObject(DistributedExecutionIndex distributedExecutionIndex) {
        return deiFaultsInjected.get(distributedExecutionIndex);
    }

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
        private static final String WARNINGS_KEY = "warnings";
        private static final String GENERATED_ID_KEY = "generated_id";
        private static final String UUID_KEY = "uuid";

    }

    public static void addAnalyzer(Class<? extends TestExecutionReportAnalyzer> clazz) {
        testExecutionReportAnalyzers.add(clazz);
    }

    public static ArrayList<Class<? extends TestExecutionReportAnalyzer>> testExecutionReportAnalyzers = new ArrayList<>();

    static {
        testExecutionReportAnalyzers.add(RedundantRPCAnalyzer.class);
        testExecutionReportAnalyzers.add(UnimplementedFailuresAnalyzer.class);
        testExecutionReportAnalyzers.add(ResponseBecomesRequestAnalyzer.class);
        testExecutionReportAnalyzers.add(MultipleInvocationsForIndividualMutationsAnalyzer.class);
        testExecutionReportAnalyzers.add(IncompleteRPCAnalyzer.class);
    }

    @SuppressWarnings("MemberName")
    private JSONObject toJSONObject() {
        for (Class<? extends TestExecutionReportAnalyzer> clazz : testExecutionReportAnalyzers) {
            try {
                TestExecutionReportAnalyzer testExecutionReportAnalyzer = clazz.getDeclaredConstructor(TestExecutionReport.class).newInstance(this);
                warnings.addAll(testExecutionReportAnalyzer.analyze(testExecutionPassed));
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new FilibusterAnalysisFailureException("could not instantiate class " + clazz + " for analysis", e);
            }
        }

        ArrayList<JSONObject> RPCs = new ArrayList<>();

        // This is totally faked because it should come from the report.  Effectively, we're using the array index here of invocation order.
        int generatedId = 0;

        for (DistributedExecutionIndex dei : deiInvocationOrder) {
            List<JSONObject> warningObjects = new ArrayList<>();

            for (FilibusterAnalyzerWarning warning : warnings) {
                if (warning.getDistributedExecutionIndex().equals(dei)) {
                    JSONObject warningObject = new JSONObject();
                    warningObject.put(Keys.DEI_KEY, warning.getDistributedExecutionIndex().toString());
                    warningObject.put("name", warning.getName());
                    warningObject.put("recommendation", warning.getRecommendations());
                    warningObject.put("impact", warning.getImpact());
                    warningObject.put("description", warning.getDescription());
                    warningObject.put("details", warning.getDetails());
                    warningObjects.add(warningObject);
                }
            }

            generatedId++;

            JSONObject RPC = new JSONObject();
            RPC.put(Keys.GENERATED_ID_KEY, String.valueOf(generatedId));
            RPC.put(Keys.DEI_KEY, dei.toString());
            RPC.put(Keys.REQUEST_KEY, deiInvocations.getOrDefault(dei, new JSONObject()));
            RPC.put(Keys.RESPONSE_KEY, deiResponses.getOrDefault(dei, new JSONObject()));
            RPC.put(Keys.FAULT_KEY, deiFaultsInjected.getOrDefault(dei, new JSONObject()));
            RPC.put(Keys.WARNINGS_KEY, warningObjects);
            RPCs.add(RPC);
        }

        JSONObject result = new JSONObject();
        result.put(Keys.ITERATION_KEY, testExecutionNumber);
        result.put(Keys.STATUS_KEY, testExecutionPassed);
        result.put(Keys.RPCS_KEY, RPCs);
        result.put(Keys.UUID_KEY, uuid);

        return result;
    }

    private String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var analysis = " + jsonObject.toString(4) + ";";
    }

    public void writePlaceholderTestReport()
    {
        try {
            // Create new directory for analysis report.
            Path directory = Paths.get("/tmp/filibuster/filibuster-test-execution-" + uuid);
            Files.createDirectory(directory);

            // Write out index file.
            Path indexPath = Paths.get(directory + "/index.html");
            byte[] indexBytes = getResourceAsBytes("html/test_execution_report/index.html");
            Files.write(indexPath, indexBytes);

            logger.info(
                    "" + "\n" +
                            "[FILIBUSTER-CORE]: Placeholder Test Execution Report written to file://" + indexPath + "\n");
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out placeholder test report: ", e);
        }
    }

    public void writeTestReport(int currentIteration, boolean exceptionOccurred) {
        testExecutionNumber = currentIteration;
        testExecutionPassed = !exceptionOccurred;

        if (!hasReportBeenMaterialized) {
            try {
                // Create new directory for analysis report.
                Path directory = Paths.get("/tmp/filibuster/filibuster-test-execution-" + uuid);
                Path scriptPath = Paths.get(directory + "/analysis.js");
                Path indexPath = Paths.get(directory + "/index.html");
                if (!Files.exists(directory))
                {
                    logger.warning("\n[FILIBUSTER-CORE] Could not find placeholder directory");
                    Files.createDirectory(directory);
                }
                if(!Files.exists(indexPath))
                {
                    logger.warning("\n[FILIBUSTER-CORE] Placeholder directory path doesn't have index.html");
                    byte[] indexBytes = getResourceAsBytes("html/test_execution_report/index.html");
                    Files.write(indexPath, indexBytes);
                }

                // Note by default Files.write overwrites existing files or create them if it doesn not exist.
                Files.write(scriptPath, toJavascript().getBytes(Charset.defaultCharset()));

                // Set materialized and it's location.
                hasReportBeenMaterialized = true;
                materializedReportMetadata = new MaterializedReportMetadata(testExecutionNumber, testExecutionPassed, indexPath, uuid);

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
}
