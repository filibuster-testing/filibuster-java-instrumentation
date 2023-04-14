package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import org.json.JSONObject;

import java.util.UUID;

@SuppressWarnings("Varifier")
public class ConcreteTestExecution extends TestExecution implements Cloneable {
    private final TestExecutionReport testExecutionReport;

    public ConcreteTestExecution(String testName, UUID testUUID) {
        testExecutionReport = new TestExecutionReport(testName, testUUID);
    }

    public ConcreteTestExecution(AbstractTestExecution abstractTestExecution, String testName, UUID testUUID) {
        testExecutionReport = new TestExecutionReport(testName, testUUID);
        faultsToInject.putAll(abstractTestExecution.faultsToInject);
        testExecutionReport.setFaultsInjected(faultsToInject);
    }

    public AbstractTestExecution toAbstractTestExecution() {
        AbstractTestExecution abstractTestExecution = new AbstractTestExecution(this);
        abstractTestExecution.executedRPCs.putAll(executedRPCs);
        abstractTestExecution.nondeterministicExecutedRPCs.putAll(nondeterministicExecutedRPCs);
        abstractTestExecution.faultsToInject.putAll(faultsToInject);
        return abstractTestExecution;
    }

    public TestExecutionReport getTestExecutionReport() {
        return testExecutionReport;
    }

    public void writePlaceHolderTestExecutionReport() {
        if (testExecutionReport != null) {
            testExecutionReport.writePlaceholderTestReport();
        }
    }

    public void writeTestExecutionReport(int currentIteration, boolean exceptionOccurred) {
        if (testExecutionReport != null) {
            testExecutionReport.writeTestReport(currentIteration, exceptionOccurred);
        }
    }

    @Override
    protected Object clone() {
        ConcreteTestExecution concreteTestExecution = new ConcreteTestExecution(testExecutionReport.getTestName(), testExecutionReport.getTestUUID());
        concreteTestExecution.generatedId = this.generatedId;
        concreteTestExecution.firstRequestSeenByService.putAll(firstRequestSeenByService);
        concreteTestExecution.executedRPCs.putAll(executedRPCs);
        concreteTestExecution.nondeterministicExecutedRPCs.putAll(nondeterministicExecutedRPCs);
        concreteTestExecution.faultsToInject.putAll(faultsToInject);
        return concreteTestExecution;
    }

    @Override
    public void addDistributedExecutionIndexWithRequestPayload(DistributedExecutionIndex distributedExecutionIndex, JSONObject payload) {
        testExecutionReport.recordInvocation(distributedExecutionIndex, payload);
        super.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, payload);
    }

    @Override
    public void addDistributedExecutionIndexWithResponsePayload(DistributedExecutionIndex distributedExecutionIndex, JSONObject payload) {
        testExecutionReport.recordInvocationComplete(distributedExecutionIndex, payload);
        super.addDistributedExecutionIndexWithResponsePayload(distributedExecutionIndex, payload);
    }
}
