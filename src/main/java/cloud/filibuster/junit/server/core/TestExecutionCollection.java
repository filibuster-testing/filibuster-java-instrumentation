package cloud.filibuster.junit.server.core;

import cloud.filibuster.junit.server.core.test_executions.TestExecution;

import java.util.Collection;

public interface TestExecutionCollection<T extends TestExecution> extends Collection<T> {
    default boolean containsAbstractTestExecution(TestExecution te) {
        for (T t : this) {
            if (t.matchesAbstractTestExecution(te)) {
                return true;
            }
        }

        return false;
    }

    T remove();
}
