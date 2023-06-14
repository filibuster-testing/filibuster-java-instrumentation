package cloud.filibuster.junit.assertions;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.junit.server.core.FilibusterCore;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

public class Helpers {

    /** Setup block used by test, to indicate setup code that calls APIs where fault injection should not be used.
     *
     * @param block code to execute.
     */
    public static void setupBlock(Runnable block) {
        incrementTestScopeCounter(BlockType.SETUP);
        disableFaultInjection();
        block.run();
        enableFaultInjection();
    }

    /** Test block used by test.
     *
     * @param block code to execute.
     */
    public static void testBlock(Runnable block) {
        incrementTestScopeCounter(BlockType.TEST);
        block.run();
    }

    /** Assertion block used by test, to indicate assertion code that calls APIs where fault injection should not be used.
     *
     * @param block code to execute.
     */
    public static void assertionBlock(Runnable block) {
        incrementTestScopeCounter(BlockType.ASSERTION);
        disableFaultInjection();
        block.run();
        enableFaultInjection();
    }

    /** Teardown block used by test, to indicate teardown code that calls APIs where fault injection should not be used.
     *
     * @param block code to execute.
     */
    public static void teardownBlock(Runnable block) {
        incrementTestScopeCounter(BlockType.TEARDOWN);
        disableFaultInjection();
        block.run();
        enableFaultInjection();
    }

    // Increment the fault-scope counter, which is just a counter of how many assertion blocks
    // we have entered via the test.
    private static void incrementTestScopeCounter(BlockType blockType) {
        // TODO: use the block type as the name.

        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().incrementTestScopeCounter(blockType);
            } else {
                throw new FilibusterUnsupportedAPIException("Unable to execute test; @TestWithFilibuster was used but no instance of Core could be found.");
            }
        } else {
            throw new FilibusterUnsupportedAPIException("Unable to execute test; Filibuster must be enabled using @TestWithFilibuster and a supported backend must be supplied.");
        }
    }

    // Turn off fault injection until explicitly re-enabled.
    private static void disableFaultInjection() {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().disableFaultInjection();
            } else {
                throw new FilibusterUnsupportedAPIException("Unable to execute test; @TestWithFilibuster was used but no instance of Core could be found.");
            }
        } else {
            throw new FilibusterUnsupportedAPIException("Unable to execute test; Filibuster must be enabled using @TestWithFilibuster and a supported backend must be supplied.");
        }
    }

    // Turn on fault injection until explicitly disabled.
    private static void enableFaultInjection() {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().enableFaultInjection();
            } else {
                throw new FilibusterUnsupportedAPIException("Unable to execute test; @TestWithFilibuster was used but no instance of Core could be found.");
            }
        } else {
            throw new FilibusterUnsupportedAPIException("Unable to execute test; Filibuster must be enabled using @TestWithFilibuster and a supported backend must be supplied.");
        }
    }
}
