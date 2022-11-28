package cloud.filibuster.junit.interceptors;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerAPI;
import com.linecorp.armeria.client.WebClient;
import org.junit.jupiter.api.extension.InvocationInterceptor;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilibusterInvocationInterceptorHelpers {
    private static final Logger logger = Logger.getLogger(FilibusterInvocationInterceptorHelpers.class.getName());

    private FilibusterInvocationInterceptorHelpers() {

    }

    @SuppressWarnings("InterruptedExceptionSwallowed")
    public static boolean shouldBypassExecution(WebClient webClient, int currentIteration, String caller) {
        try {
            return !FilibusterServerAPI.hasNextIteration(webClient, currentIteration, caller);
        } catch (Exception e) {
            // TODO: fix me: in event server is unavailable, skip all executions except the first one.
            return true;
        }
    }

    @SuppressWarnings("InterruptedExceptionSwallowed")
    public static void proceedAndLogException(InvocationInterceptor.Invocation<Void> invocation,
                                              int currentIteration,
                                              WebClient webClient,
                                              FilibusterConfiguration filibusterConfiguration) throws Throwable {
        try {
            invocation.proceed();
            FilibusterServerAPI.recordIterationComplete(webClient, currentIteration, /* exceptionOccurred= */false);
        } catch (Throwable t) {
            FilibusterServerAPI.recordIterationComplete(webClient, currentIteration, /* exceptionOccurred= */true);
            throw t;
        }
    }

    /**
     * Conditionally mark the teardown for a given test iteration complete.
     *
     * There is a significant amount of nuance in this function.  The Filibuster server needs to know when a particular
     * test iteration is done and all the teardown is complete.  This is non-trivial because test functions might contain
     * and arbitrary number of afterEach methods (or, none at all.)  Therefore, the only way to ensure that we capture this is to do the following.
     *
     * First, instrument the beforeEach and the testTemplate method.  Record a boolean in a map to indicate that the previous test is
     * complete when we reach the start of a new test.  However, some tests might have a beforeEach executed in the first iteration,
     * therefore, we also need to prevent notifying the server on the first iteration (iteration 0, the start of the first actual test.)
     *
     * @param invocationCompletionMap tracks the invocations that have completed and all teardowns have finished.
     * @param currentIteration the iteration we are currently in, not the iteration that's been completed (current - 1).
     * @param webClient a web client to use to talk to the Filibuster Server.
     */
    public static void conditionallyMarkTeardownComplete(HashMap<Integer, Boolean> invocationCompletionMap, int currentIteration, WebClient webClient) {
        int previousIteration = currentIteration - 1;

        if (! invocationCompletionMap.containsKey(previousIteration) && (previousIteration != 0)) {
            try {
                FilibusterServerAPI.teardownsCompleted(webClient, previousIteration);
            } catch (ExecutionException | InterruptedException e) {
                logger.log(Level.SEVERE, "Could not notify Filibuster of teardown completed; this is fatal error: " + e);
            }
            invocationCompletionMap.put(previousIteration, true);
        }
    }
}
