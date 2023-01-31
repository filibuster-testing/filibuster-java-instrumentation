package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.Map;

@SuppressWarnings("Varifier")
public class ConcreteTestExecution extends TestExecution implements Cloneable {
    public ConcreteTestExecution() {

    }

    public ConcreteTestExecution(AbstractTestExecution abstractTestExecution) {
        for (Map.Entry<DistributedExecutionIndex, JSONObject> faultToInject : abstractTestExecution.faultsToInject.entrySet()) {
            faultsToInject.put(faultToInject.getKey(), faultToInject.getValue());
        }
    }

    public AbstractTestExecution toAbstractTestExecution() {
        AbstractTestExecution abstractTestExecution = new AbstractTestExecution(this);

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : executedRPCs.entrySet()) {
            abstractTestExecution.executedRPCs.put(mapEntry.getKey(), mapEntry.getValue());
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : nondeterministicExecutedRPCs.entrySet()) {
            abstractTestExecution.nondeterministicExecutedRPCs.put(mapEntry.getKey(), mapEntry.getValue());
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : faultsToInject.entrySet()) {
            abstractTestExecution.faultsToInject.put(mapEntry.getKey(), mapEntry.getValue());
        }

        return abstractTestExecution;
    }

    @Override
    protected Object clone() {
        ConcreteTestExecution concreteTestExecution = new ConcreteTestExecution();

        concreteTestExecution.generatedId = this.generatedId;

        for (Map.Entry<String, Boolean> mapEntry : firstRequestSeenByService.entrySet()) {
            concreteTestExecution.firstRequestSeenByService.put(mapEntry.getKey(), mapEntry.getValue());
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : executedRPCs.entrySet()) {
            concreteTestExecution.executedRPCs.put(mapEntry.getKey(), mapEntry.getValue());
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : nondeterministicExecutedRPCs.entrySet()) {
            concreteTestExecution.nondeterministicExecutedRPCs.put(mapEntry.getKey(), mapEntry.getValue());
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> mapEntry : faultsToInject.entrySet()) {
            concreteTestExecution.faultsToInject.put(mapEntry.getKey(), mapEntry.getValue());
        }

        return concreteTestExecution;
    }
}
