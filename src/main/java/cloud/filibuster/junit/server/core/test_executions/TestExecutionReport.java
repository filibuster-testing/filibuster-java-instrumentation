package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterTestReportWriterException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class TestExecutionReport {
    private static final Logger logger = Logger.getLogger(TestExecutionReport.class.getName());

    private final ArrayList<DistributedExecutionIndex> deiInvocationOrder = new ArrayList<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiInvocations = new HashMap<>();

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

    public void writeTestReport() {
        try {
            Path path = Files.createTempFile(generateFilename(), ".json");
            Files.write(path, toJSONObject().toString(4).getBytes());
            logger.info(
                    "" + "\n" +
                            "[FILIBUSTER-CORE]: Test Execution Report written to file://" + path + "\n");
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
}
