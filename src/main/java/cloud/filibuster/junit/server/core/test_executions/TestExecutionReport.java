package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
public class TestExecutionReport {
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
}
