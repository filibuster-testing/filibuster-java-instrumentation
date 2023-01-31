package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.Stack;

public class TestExecutionStack<T extends TestExecution> extends Stack<T> implements TestExecutionCollection<T> {
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
        return this.pop();
    }

    @Override
    public void addTestExecution(T testExecution) {
        this.push(testExecution);
    }
}
