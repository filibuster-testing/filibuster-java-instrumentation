package cloud.filibuster.junit.assertions;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedByHTTPServerException;
import cloud.filibuster.junit.server.core.FilibusterCore;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

public class Grpc {

    /**
     * Execute the following block without faults.
     * <p>
     * Assumes that the block is executed synchronously -- use of this inside of concurrency primitives without
     * explicit synchronization may render this function unable to prevent faults from being injected.
     *
     * @param block block to execute synchronously.
     */
    public static void executeGrpcWithoutFaults(Runnable block) {
        incrementFaultScopeCounter();
        disableFaultInjection();
        block.run();
        enableFaultInjection();
    }

    // Increment the fault-scope counter, which is just a counter of how many assertion blocks
    // we have entered via the test.
    private static void incrementFaultScopeCounter() {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().incrementFaultScopeCounter();
            }

            // no-op, otherwise, so this has no effect when fault injection is off (i.e., @Test)
        } else {
            throw new FilibusterUnsupportedByHTTPServerException("incrementFaultScopeCounter only supported with local server.");
        }
    }

    // Turn off fault injection until explicitly re-enabled.
    private static void disableFaultInjection() {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().disableFaultInjection();
            }

            // no-op, otherwise, so this has no effect when fault injection is off (i.e., @Test)
        } else {
            throw new FilibusterUnsupportedByHTTPServerException("disableFaultInjection only supported with local server.");
        }
    }

    // Turn on fault injection until explicitly disabled.
    private static void enableFaultInjection() {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().enableFaultInjection();
            }

            // no-op, otherwise, so this has no effect when fault injection is off (i.e., @Test)
        } else {
            throw new FilibusterUnsupportedByHTTPServerException("enableFaultInjection only supported with local server.");
        }
    }
}
