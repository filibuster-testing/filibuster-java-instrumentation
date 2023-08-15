package cloud.filibuster.junit.server.core.test_executions;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.assertions.BlockType;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("Varifier")
public class ConcreteTestExecution extends TestExecution implements Cloneable {
    private final TestExecutionReport testExecutionReport;

    private int testScopeCounter = 0;

    private BlockType lastTestScopeBlockType = BlockType.DEFAULT;

    public ConcreteTestExecution(String testName, UUID testUUID, String className) {
        testExecutionReport = new TestExecutionReport(testName, testUUID, className);
    }

    public ConcreteTestExecution(AbstractTestExecution abstractTestExecution, String testName, UUID testUUID, String className) {
        testExecutionReport = new TestExecutionReport(testName, testUUID, className);
        faultsToInject.putAll(abstractTestExecution.faultsToInject);
        testExecutionReport.setFaultsInjected(faultsToInject);
    }

    public Map<DistributedExecutionIndex, JSONObject> getFaultsToInject() {
        return this.faultsToInject;
    }

    public Map<DistributedExecutionIndex, JSONObject> getFailedRPCs() {
        return this.failedRPCs;
    }

    public Map<DistributedExecutionIndex, JSONObject> getExecutedRPCs() {
        return this.executedRPCs;
    }

    public void incrementTestScopeCounter() {
        testScopeCounter++;
    }

    public void incrementTestScopeCounter(BlockType blockType) {
        testScopeCounter++;
        lastTestScopeBlockType = blockType;
    }

    public int getTestScopeCounter() {
        return testScopeCounter;
    }

    public BlockType getLastTestScopeBlockType() {
        return lastTestScopeBlockType;
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

    public void writeTestExecutionReport(int currentIteration, boolean exceptionOccurred, @Nullable Throwable throwable) {
        if (testExecutionReport != null) {
            testExecutionReport.writeTestReport(currentIteration, exceptionOccurred, throwable);
        }
    }

    @Override
    protected Object clone() {
        ConcreteTestExecution concreteTestExecution = new ConcreteTestExecution(testExecutionReport.getTestName(),
                testExecutionReport.getTestUUID(),
                testExecutionReport.getClassName());
        concreteTestExecution.generatedId = this.generatedId;
        concreteTestExecution.firstRequestSeenByService.putAll(firstRequestSeenByService);
        concreteTestExecution.executedRPCs.putAll(executedRPCs);
        concreteTestExecution.nondeterministicExecutedRPCs.putAll(nondeterministicExecutedRPCs);
        concreteTestExecution.faultsToInject.putAll(faultsToInject);
        return concreteTestExecution;
    }

    @Override
    public void addDistributedExecutionIndexWithRequestPayload(DistributedExecutionIndex distributedExecutionIndex, JSONObject payload) {
        addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, payload, /* seen= */ false);
    }

    public void addDistributedExecutionIndexWithRequestPayload(DistributedExecutionIndex distributedExecutionIndex, JSONObject payload, boolean seen) {
        testExecutionReport.recordInvocation(distributedExecutionIndex, payload);
        super.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, payload);

        if (seen) {
            testExecutionReport.markRpcAsCached(distributedExecutionIndex);
        }
    }

    @Override
    public void addDistributedExecutionIndexWithResponsePayload(DistributedExecutionIndex distributedExecutionIndex, JSONObject payload) {
        testExecutionReport.recordInvocationComplete(distributedExecutionIndex, payload);
        super.addDistributedExecutionIndexWithResponsePayload(distributedExecutionIndex, payload);
    }
}
