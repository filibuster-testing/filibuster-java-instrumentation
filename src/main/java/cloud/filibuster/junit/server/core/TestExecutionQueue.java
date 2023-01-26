package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.concurrent.LinkedBlockingDeque;

public class TestExecutionQueue<T extends TestExecution> extends LinkedBlockingDeque<T> {
    public boolean nondeterministicContains(TestExecution te) {

        for (T t : this) {
            if (t.nondeterministicEquals(te)) {
                return true;
            }
        }

        return false;
    }
}
