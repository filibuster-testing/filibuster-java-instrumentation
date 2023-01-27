package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.Map;

public class ConcreteTestExecution extends TestExecution {
    public ConcreteTestExecution() {

    }

    public ConcreteTestExecution(AbstractTestExecution abstractTestExecution) {
        for (Map.Entry<DistributedExecutionIndex, JSONObject> faultToInject : abstractTestExecution.faultsToInject.entrySet()) {
            faultsToInject.put(faultToInject.getKey(), faultToInject.getValue());
        }
    }
}
