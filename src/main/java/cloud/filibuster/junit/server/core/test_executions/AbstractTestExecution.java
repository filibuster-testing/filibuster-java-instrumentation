package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import javax.annotation.Nullable;

import java.util.Map;

@SuppressWarnings("Varifier")
public class AbstractTestExecution extends TestExecution {
    // This is a partial execution that gets us to this invocation point where we want to inject a fault.
    @Nullable
    private ConcreteTestExecution sourceConcreteTestExecution;

    // This is a reference to the completed execution that this execution was generated from.
    // This will reference RPCs that this abstract execution does not know about -- if they happened after the this
    // abstract execution was cut.
    @Nullable
    private ConcreteTestExecution completedSourceConcreteTestExecution;

    public AbstractTestExecution() {
    }

    public AbstractTestExecution(ConcreteTestExecution concreteTestExecution) {
        this.sourceConcreteTestExecution = (ConcreteTestExecution) concreteTestExecution.clone();
        this.completedSourceConcreteTestExecution = concreteTestExecution;
    }

    public boolean sawInConcreteTestExecution(
            DistributedExecutionIndex distributedExecutionIndex
    ) {
        return sawInConcreteTestExecution(this.sourceConcreteTestExecution, distributedExecutionIndex);
    }

    public boolean shoulBypassForOrganicFailure() {
        boolean found = false;

        if (sourceConcreteTestExecution != null) {
            for (Map.Entry<DistributedExecutionIndex, JSONObject> executedRPC: sourceConcreteTestExecution.executedRPCs.entrySet()) {
                DistributedExecutionIndex distributedExecutionIndex = executedRPC.getKey();
                if (organicallyFailedInSourceConcreteTestExecution(this.sourceConcreteTestExecution, this.completedSourceConcreteTestExecution, distributedExecutionIndex) && faultsToInject.containsKey(distributedExecutionIndex)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    public void addFaultToInject(DistributedExecutionIndex distributedExecutionIndex, JSONObject faultObject) {
        faultsToInject.put(distributedExecutionIndex, faultObject);
    }

    public int getFaultsToInjectSize() {
        return this.faultsToInject.size();
    }
    
}
