package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

public interface TestExecutionCollection<T extends TestExecution> {
    boolean containsAbstractTestExecution(TestExecution te);

    boolean containsTestExecution(T testExecution);

    boolean isEmpty();

    T removeAndReturnNextTestExecution();

    int size();

    void addTestExecution(T testExecution);
}
