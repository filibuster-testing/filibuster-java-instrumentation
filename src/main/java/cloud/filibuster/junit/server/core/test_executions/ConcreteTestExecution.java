package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import org.json.JSONObject;

import java.util.Map;

@SuppressWarnings("Varifier")
public class ConcreteTestExecution extends TestExecution implements Cloneable {
    private final TestExecutionReport testExecutionReport = new TestExecutionReport();

    public ConcreteTestExecution() {

    }

    public ConcreteTestExecution(AbstractTestExecution abstractTestExecution) {
        faultsToInject.putAll(abstractTestExecution.faultsToInject);
    }

    public AbstractTestExecution toAbstractTestExecution() {
        AbstractTestExecution abstractTestExecution = new AbstractTestExecution(this);
        abstractTestExecution.executedRPCs.putAll(executedRPCs);
        abstractTestExecution.nondeterministicExecutedRPCs.putAll(nondeterministicExecutedRPCs);
        abstractTestExecution.faultsToInject.putAll(faultsToInject);
        return abstractTestExecution;
    }

    @Override
    protected Object clone() {
        ConcreteTestExecution concreteTestExecution = new ConcreteTestExecution();
        concreteTestExecution.generatedId = this.generatedId;
        concreteTestExecution.firstRequestSeenByService.putAll(firstRequestSeenByService);
        concreteTestExecution.executedRPCs.putAll(executedRPCs);
        concreteTestExecution.nondeterministicExecutedRPCs.putAll(nondeterministicExecutedRPCs);
        concreteTestExecution.faultsToInject.putAll(faultsToInject);
        return concreteTestExecution;
    }

    public TestExecutionReport getTestExecutionReport() {
        return testExecutionReport;
    }
}
