package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.Map;

// A valid test execution derived from a partial test execution once executed.
public class ConcreteTestExecution extends TestExecution {

    public ConcreteTestExecution() {

    }

    public ConcreteTestExecution(PartialTestExecution partialTestExecution) {
        for (Map.Entry<DistributedExecutionIndex, JSONObject> faultToInject : partialTestExecution.faultsToInject.entrySet()) {
            faultsToInject.put(faultToInject.getKey(), faultToInject.getValue());
        }
    }

    public ConcreteTestExecution(PartialTestExecution partialTestExecution, boolean shouldCopyRPCs) {
        for (Map.Entry<DistributedExecutionIndex, JSONObject> faultToInject : partialTestExecution.faultsToInject.entrySet()) {
            faultsToInject.put(faultToInject.getKey(), faultToInject.getValue());
        }

        if (shouldCopyRPCs) {
            for (Map.Entry<DistributedExecutionIndex, JSONObject> executedRPC : partialTestExecution.executedRPCs.entrySet()) {
                executedRPCs.put(executedRPC.getKey(), executedRPC.getValue());
            }
        }
    }

}
