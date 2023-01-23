package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

// A valid prefix of a test execution used for fault injection.
//
// By exploring this test execution, the program may or may not reveal more RPCs, based on whether
// the program has failure handling for the injected fault.  This test execution is used to reveal a
// concrete execution which represents an actual valid program execution containing a fault.
public class PartialTestExecution extends TestExecution {
    public void addFaultToInject(DistributedExecutionIndex distributedExecutionIndex, JSONObject faultObject) {
        faultsToInject.put(distributedExecutionIndex, faultObject);
    }
}
