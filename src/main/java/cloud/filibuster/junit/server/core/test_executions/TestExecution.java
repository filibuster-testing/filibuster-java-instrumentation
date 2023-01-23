package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class TestExecution {
    private static final Logger logger = Logger.getLogger(TestExecution.class.getName());

    // Legacy value used to number the RPCs for fault injection.
    // Superceded by DistributedExecutionIndex, but kept in for compatibility and debugging.
    int generatedId = 0;

    // What RPCs were executed?
    HashMap<DistributedExecutionIndex, JSONObject> executedRPCs = new HashMap<>();

    // What faults should be injected in this execution?
    HashMap<DistributedExecutionIndex, JSONObject> faultsToInject = new HashMap<>();

    public void printRPCs() {
        logger.info("RPCs executed and interposed by Filibuster:");

        for (DistributedExecutionIndex name: executedRPCs.keySet()) {
            String key = name.toString();
            String value = executedRPCs.get(name).toString(4);
            logger.info(key + " => " + value);
        }

        if (!faultsToInject.isEmpty()) {
            logger.info("Faults injected by Filibuster:");

            for (DistributedExecutionIndex name: faultsToInject.keySet()) {
                String key = name.toString();
                String value = faultsToInject.get(name).toString(4);
                logger.info(key + " => " + value);
            }
        } else {
            logger.info("No faults injected by Filibuster:");
        }
    }

    public int addDistributedExecutionIndexWithPayload(DistributedExecutionIndex distributedExecutionIndex, JSONObject payload) {
        // Remove fields that are only related to the logging and don't contain useful information.
        payload.remove("instrumentation_type");

        // Increment the generated_id; not used for anything anymore and merely here for debugging and because callers require it.
        generatedId++;

        // Add to the list of executed RPCs.
        executedRPCs.put(distributedExecutionIndex, payload);

        return generatedId;
    }

    public boolean shouldFault(DistributedExecutionIndex distributedExecutionIndex) {
        if (this.faultsToInject.containsKey(distributedExecutionIndex)) {
            return true;
        } else {
            return false;
        }
    }

    public JSONObject getFault(DistributedExecutionIndex distributedExecutionIndex) {
        return this.faultsToInject.get(distributedExecutionIndex);
    }

    public PartialTestExecution cloneToPartialTestExecution() {
        PartialTestExecution partialTestExecution = new PartialTestExecution();

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : executedRPCs.entrySet()) {
            partialTestExecution.executedRPCs.put(mapEntry.getKey(), mapEntry.getValue());
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : faultsToInject.entrySet()) {
            partialTestExecution.faultsToInject.put(mapEntry.getKey(), mapEntry.getValue());
        }

        return partialTestExecution;
    }

    public boolean wasFaultInjected() {
        return !this.faultsToInject.isEmpty();
    }

    private boolean wasFaultInjectedMatcher(String searchField, String stringToFind) {
        for (Map.Entry<DistributedExecutionIndex, JSONObject> entry : executedRPCs.entrySet()) {
            JSONObject jsonObject = entry.getValue();

            if (jsonObject.has(searchField)) {
                String field = jsonObject.getString(searchField);
                if (field.contains(stringToFind)) {
                    DistributedExecutionIndex distributedExecutionIndex = entry.getKey();
                    if (faultsToInject.containsKey(distributedExecutionIndex)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean wasFaultInjectedOnService(String serviceName) {
        return wasFaultInjectedMatcher("module", serviceName);
    }

    // Recombination of RPC is artifact of HTTP API.
    public boolean wasFaultInjectedOnMethod(String serviceName, String methodName) {
        return wasFaultInjectedMatcher("method", serviceName + "/" + methodName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TestExecution)) {
            return false;
        }

        TestExecution te = (TestExecution) o;

        // Are the key sets equivalent?
        if (!this.executedRPCs.keySet().equals(te.executedRPCs.keySet())) {
            return false;
        }

        // Are the JSON objects similar for each key?
        boolean equalRPCsMap = this.executedRPCs.entrySet().stream().allMatch(e -> e.getValue().similar(te.executedRPCs.get(e.getKey())));

        // Are the key sets equivalent?
        if (!this.faultsToInject.keySet().equals(te.faultsToInject.keySet())) {
            return false;
        }

        // Are the JSON objects similar for each key?
        boolean equalFaultToInjectMap = this.faultsToInject.entrySet().stream().allMatch(e -> e.getValue().similar(te.faultsToInject.get(e.getKey())));

        return equalRPCsMap && equalFaultToInjectMap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.executedRPCs, this.faultsToInject);
    }
}
