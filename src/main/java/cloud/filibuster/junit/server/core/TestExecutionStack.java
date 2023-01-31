package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.ArrayDeque;
import java.util.Stack;

public class TestExecutionStack<T extends TestExecution> extends ArrayDeque<T> implements TestExecutionCollection<T> {
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
        return this.removeFirst();
    }

    @Override
    public void addTestExecution(T testExecution) {
        this.addFirst(testExecution);
    }
}
