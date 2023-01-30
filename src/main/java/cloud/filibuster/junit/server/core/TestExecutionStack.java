package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.Stack;

public class TestExecutionStack<T extends TestExecution> extends Stack<T> implements TestExecutionCollection<T> {
    @Override
    public T remove() {
        return this.pop();
    }
}
