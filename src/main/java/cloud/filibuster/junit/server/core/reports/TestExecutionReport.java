package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterAnalysisFailureException;
import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import cloud.filibuster.junit.server.core.lint.analyzers.test_execution_report.*;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class TestExecutionReport {
    private static final Logger logger = Logger.getLogger(TestExecutionReport.class.getName());

    private boolean hasReportBeenMaterialized = false;

    @Nullable
    private MaterializedTestExecutionReportMetadata materializedTestExecutionReportMetadata;

    private int testExecutionNumber = 0;

    private boolean testExecutionPassed = false;

    private final ArrayList<DistributedExecutionIndex> deiInvocationOrder = new ArrayList<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiInvocations = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiResponses = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiFaultsInjected = new HashMap<>();

    private final List<FilibusterAnalyzerWarning> warnings = new ArrayList<>();

    private final UUID uuid = UUID.randomUUID();

    private final UUID testUUID;

    private final String testName;

    public TestExecutionReport(String testName, UUID testUUID) {
        this.testName = testName;
        this.testUUID = testUUID;
    }

    private File getDirectoryPath() {
        return new File(ReportUtilities.getBaseDirectoryPath(), "filibuster-test-" + testUUID.toString());
    }

    private File getSubdirectoryPath() {
        return new File(getDirectoryPath(), "filibuster-test-execution-" + uuid);
    }


    public boolean isTestExecutionPassed() {
        return testExecutionPassed;
    }

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

    public String getTestName() {
        return this.testName;
    }

    public UUID getTestUUID() {
        return this.testUUID;
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
        private static final String TEST_NAME = "test_name";

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
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
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
        result.put(Keys.TEST_NAME, testName);

        return result;
    }

    private String toJavascript() {
        JSONObject jsonObject = toJSONObject();
        return "var analysis = " + jsonObject.toString(4) + ";";
    }

    public void writePlaceholderTestReport() {
        // Create new directory for analysis report.
        File directory = getSubdirectoryPath();
        try {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();

            // Write out index file.
            Path indexPath = Paths.get(directory + "/index.html");
            byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(), "html/test_execution_report/index.html");
            Files.write(indexPath, indexBytes);

            logger.info(
                    "" + "\n" +
                            "[FILIBUSTER-CORE]: Placeholder Test Execution Report written to file://" + indexPath + "\n");
        } catch (IOException e) {
            throw new FilibusterTestReportWriterException("Filibuster failed to write out placeholder test execution report: ", e);
        }
    }

    public void writeTestReport(int currentIteration, boolean exceptionOccurred) {
        testExecutionNumber = currentIteration;
        testExecutionPassed = !exceptionOccurred;

        if (!hasReportBeenMaterialized) {
            try {
                // Create new directory for analysis report.
                Path directory = getSubdirectoryPath().toPath();
                Path scriptPath = Paths.get(directory + "/analysis.js");
                Path indexPath = Paths.get(directory + "/index.html");
                if (!Files.exists(directory)) {
                    logger.warning("\n[FILIBUSTER-CORE] Could not find placeholder directory");
                    Files.createDirectory(directory);
                }
                if (!Files.exists(indexPath)) {
                    logger.warning("\n[FILIBUSTER-CORE] Placeholder directory path doesn't have index.html");
                    byte[] indexBytes = ReportUtilities.getResourceAsBytes(getClass().getClassLoader(),
                            "html/test_execution_report/index.html");
                    Files.write(indexPath, indexBytes);
                }

                // Note by default Files.write overwrites existing files or create them if it does not exist.
                Files.write(scriptPath, toJavascript().getBytes(Charset.defaultCharset()));

                // Set materialized and it's location.
                hasReportBeenMaterialized = true;
                materializedTestExecutionReportMetadata = new MaterializedTestExecutionReportMetadata(testExecutionNumber, testExecutionPassed, indexPath, uuid);

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

    public MaterializedTestExecutionReportMetadata getMaterializedReportMetadata() {
        return this.materializedTestExecutionReportMetadata;
    }
}
