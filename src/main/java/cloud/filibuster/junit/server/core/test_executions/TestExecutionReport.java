package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
public class TestExecutionReport {

    private final ArrayList<DistributedExecutionIndex> deiInvocationOrder = new ArrayList<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiInvocations = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiInvocationRequests = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiInvocationResponses = new HashMap<>();

    private final HashMap<DistributedExecutionIndex, JSONObject> deiFaultsInjected = new HashMap<>();

    public void recordInvocationAndRequest(
            DistributedExecutionIndex distributedExecutionIndex,
            JSONObject invocationJsonObject,
            JSONObject requestJsonObject
    ) {
        // Add to invocation order list.
        deiInvocationOrder.add(distributedExecutionIndex);

        // ...then, record the information about the invocation.
        deiInvocations.put(distributedExecutionIndex, invocationJsonObject);

        // ...then, record the information about the request.
        deiInvocationRequests.put(distributedExecutionIndex, requestJsonObject);
    }

    public void recordInvocationResponse(
            DistributedExecutionIndex distributedExecutionIndex,
            JSONObject responseJsonObject
    ) {
        // Add to invocation response list.
        deiInvocationResponses.put(distributedExecutionIndex, responseJsonObject);
    }

    public void setFaultsInjected() {
        // TODO
    }

    // TODO: replace strings with constants.
    @SuppressWarnings("MemberName")
    public JSONObject toJSONObject() {
        ArrayList<JSONObject> RPCs = new ArrayList<>();

        for (DistributedExecutionIndex dei : deiInvocationOrder) {
            JSONObject RPC = new JSONObject();
            RPC.put("dei", dei.toString());
            RPC.put("details", deiInvocations.getOrDefault(dei, new JSONObject()));
            RPC.put("request", deiInvocationRequests.getOrDefault(dei, new JSONObject()));
            RPC.put("response", deiInvocationResponses.getOrDefault(dei, new JSONObject()));
            RPC.put("fault", deiFaultsInjected.getOrDefault(dei, new JSONObject()));
            RPCs.add(RPC);
        }

        JSONObject result = new JSONObject();
        result.put("rpcs", RPCs);
        return result;
    }
}
