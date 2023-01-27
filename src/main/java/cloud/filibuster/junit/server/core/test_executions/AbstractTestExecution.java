package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

public class AbstractTestExecution extends TestExecution {
    public void addFaultToInject(DistributedExecutionIndex distributedExecutionIndex, JSONObject faultObject) {
        faultsToInject.put(distributedExecutionIndex, faultObject);
    }

    public int getFaultsToInjectSize() {
        return this.faultsToInject.size();
    }
}
