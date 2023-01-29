package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class AbstractTestExecution extends TestExecution {
    @Nullable
    private ConcreteTestExecution sourceConcreteTestExecution;

    public AbstractTestExecution() {
    }

    public AbstractTestExecution(ConcreteTestExecution concreteTestExecution) {
        this.sourceConcreteTestExecution = (ConcreteTestExecution) concreteTestExecution.clone();
    }

    public boolean sawInConcreteTestExecution(DistributedExecutionIndex distributedExecutionIndex) {
        if (sourceConcreteTestExecution != null) {
            return sourceConcreteTestExecution.executedRPCs.containsKey(distributedExecutionIndex);
        }

        return false;
    }

    public void addFaultToInject(DistributedExecutionIndex distributedExecutionIndex, JSONObject faultObject) {
        faultsToInject.put(distributedExecutionIndex, faultObject);
    }

    public int getFaultsToInjectSize() {
        return this.faultsToInject.size();
    }
}
