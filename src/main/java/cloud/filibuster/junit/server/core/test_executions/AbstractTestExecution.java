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

    public boolean sawInConcreteTestExecution(DistributedExecutionIndex distributedExecutionIndex) {
        if (sourceConcreteTestExecution != null) {
            return sourceConcreteTestExecution.executedRPCs.containsKey(distributedExecutionIndex);
        }

        return false;
    }

    public boolean shoulBypassForOrganicFailure() {
        boolean found = false;

        if (sourceConcreteTestExecution != null) {
            for (Map.Entry<DistributedExecutionIndex, JSONObject> executedRPC: sourceConcreteTestExecution.executedRPCs.entrySet()) {
                DistributedExecutionIndex distributedExecutionIndex = executedRPC.getKey();
                if (organicallyFailedInSourceConcreteTestExecution(distributedExecutionIndex) && faultsToInject.containsKey(distributedExecutionIndex)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    private boolean organicallyFailedInSourceConcreteTestExecution(DistributedExecutionIndex distributedExecutionIndex) {
        // Make sure we first have a reference to the concrete execution used to construct this execution
        // as well as the completed source concrete execution from the execution this was created from.
        if (sourceConcreteTestExecution != null && completedSourceConcreteTestExecution != null) {
            // Condition 1: In the partial concrete execution, did we execute the RPC?
            //
            // Necessary, because we only care about RPCs that executed up to the point where we construct the
            // concrete execution (not all the RPCs that executed in the entire execution.)
            boolean sawInConcreteTestExecution = sawInConcreteTestExecution(distributedExecutionIndex);

            // Condition 2: Did it fail in the completed concrete execution?
            //
            // This is a dirty workaround/hack.
            //
            // We schedule faults when the RPCs are started, not finished, at the moment.
            // Therefore, we don't know the response.
            //
            // To get around this and find out if an RPC failed in the execution we keep a reference
            // to the concrete execution that contains all RPCs and responses that the abstract was built from.
            // Checking this alone isn't enough, because we want to restrict ourselves only to the subset of RPCs that
            // had been started when we built the abstract execution, hence the prior check.
            boolean failedInConcreteTestExecution = completedSourceConcreteTestExecution.failedRPCs.containsKey(distributedExecutionIndex);

            // Condition 3: Did we inject a fault and that's the reason for failure?
            //
            // Finally, we must ensure that the RPC didn't fail because of a fault that we injected.
            boolean wasNotFaultInjected = !sourceConcreteTestExecution.faultsToInject.containsKey(distributedExecutionIndex);

            // If all three conditions are true, then we know it failed in the execution for a non-FI reason.
            //
            // In short, if the partial execution that got me to the point of making this abstract execution
            // experienced a failure -- and I did not cause that failure -- it must have happened for another reason.
            //
            if (sawInConcreteTestExecution && failedInConcreteTestExecution && wasNotFaultInjected) {
                return true;
            }
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
