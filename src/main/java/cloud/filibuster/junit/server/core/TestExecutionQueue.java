package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.concurrent.LinkedBlockingDeque;

public class TestExecutionQueue<T extends TestExecution> extends LinkedBlockingDeque<T> {
    public boolean containsAbstractTestExecution(TestExecution te) {
        for (T t : this) {
            if (t.matchesAbstractTestExecution(te)) {
                return true;
            }
        }

        return false;
    }
}
