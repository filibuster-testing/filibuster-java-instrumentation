package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.concurrent.LinkedBlockingDeque;

public class TestExecutionQueue<T extends TestExecution> extends LinkedBlockingDeque<T> implements TestExecutionCollection<T> {
    @Override
    public boolean containsAbstractTestExecution(TestExecution te) {
        for (T t : this) {
            if (t.matchesAbstractTestExecution(te)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean containsTestExecution(T testExecution) {
        return this.contains(testExecution);
    }

    @Override
    public T removeAndReturnNextTestExecution() {
        return this.remove();
    }

    @Override
    public void addTestExecution(T testExecution) {
        this.add(testExecution);
    }
}
