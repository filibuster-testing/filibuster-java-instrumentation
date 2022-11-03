package cloud.filibuster.junit.interceptors;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Property;
import cloud.filibuster.junit.server.FilibusterServerLifecycle;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerAPI;
import com.linecorp.armeria.client.WebClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invocation Interceptor for automatically running tests with Filibuster.
 */
public class FilibusterInvocationInterceptor implements InvocationInterceptor {
    private static final Logger logger = Logger.getLogger(FilibusterInvocationInterceptor.class.getName());

    public static boolean shouldInitializeFilibusterServer = true;

    private final FilibusterConfiguration filibusterConfiguration;

    private final HashMap<Integer, Boolean> invocationCompletionMap;

    private final int currentIteration;

    private final int maxIterations;

    private final WebClient webClient;

    /**
     * Invocation interceptor for running tests with Filibuster.
     *
     * Automatically starts, stops the external Filibuster server and performs necessary IPC.
     *
     * @param filibusterConfiguration configuration of Filibuster server.
     * @param currentIteration the current iteration that is being executed.
     * @param maxIterations upper bound on allowable executions.
     * @param invocationCompletionMap tracks whether teardown has been completed for a given test.
     */
    public FilibusterInvocationInterceptor(
            FilibusterConfiguration filibusterConfiguration,
            int currentIteration,
            int maxIterations,
            HashMap<Integer, Boolean> invocationCompletionMap) {
        this.currentIteration = currentIteration;
        this.maxIterations = maxIterations;

        this.invocationCompletionMap = invocationCompletionMap;

        this.filibusterConfiguration = filibusterConfiguration;

        this.webClient = WebClient.builder(filibusterConfiguration.getFilibusterBaseUri())
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .build();
    }

    /****************************************************************************************************************
     * Filibuster system properties.
     */

    static class FilibusterSystemProperties {
        public static void setSystemPropertiesForFilibusterInstrumentation(FilibusterConfiguration filibusterConfiguration) {
            if (filibusterConfiguration.getDataNondeterminism()) {
                DistributedExecutionIndexV1.Properties.Asynchronous.setAsynchronousInclude(false);
            }

            System.setProperty("kotlinx.coroutines.debug", "on");
            System.setProperty("kotlinx.coroutines.stacktrace.recovery", "true");
        }

        public static void unsetSystemPropertiesForFilibusterInstrumentation() {
            DistributedExecutionIndexV1.Properties.Asynchronous.setAsynchronousInclude(true);

            System.setProperty("kotlinx.coroutines.debug", "off");
            System.setProperty("kotlinx.coroutines.stacktrace.recovery", "false");
        }
    }

    /****************************************************************************************************************
     * Helpers.
     */

    static class Helpers {
        @SuppressWarnings("InterruptedExceptionSwallowed")
        private static boolean shouldBypassExecution(WebClient webClient, int currentIteration, String caller) {
            try {
                return !FilibusterServerAPI.hasNextIteration(webClient, currentIteration, caller);
            } catch (Exception e) {
                // TODO: fix me: in event server is unavailable, skip all executions except the first one.
                return true;
            }
        }

        @SuppressWarnings("InterruptedExceptionSwallowed")
        private static void proceedAndLogException(InvocationInterceptor.Invocation<Void> invocation,
                                                   int currentIteration,
                                                   WebClient webClient) throws Throwable {
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
        private static void conditionallyMarkTeardownComplete(HashMap<Integer, Boolean> invocationCompletionMap, int currentIteration, WebClient webClient) {
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

    /****************************************************************************************************************
     * Invocation callbacks.
     */

    @Override
    public void interceptTestTemplateMethod(InvocationInterceptor.Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {
        // Start on the first iteration.
        if (currentIteration == 1) {
            FilibusterSystemProperties.setSystemPropertiesForFilibusterInstrumentation(filibusterConfiguration);

            if (shouldInitializeFilibusterServer) {
                FilibusterServerLifecycle.startServer(filibusterConfiguration, webClient);
                FilibusterServerAPI.analysisFile(webClient, filibusterConfiguration.readAnalysisFile());
            }
        }

        Helpers.conditionallyMarkTeardownComplete(invocationCompletionMap, currentIteration, webClient);

        Property.setInstrumentationEnabledProperty(true);

        // Test handling.
        if (currentIteration == 1) {
            // First iteration always runs because it's the fault free execution.
            Helpers.proceedAndLogException(invocation, currentIteration, webClient);
        } else if (currentIteration == maxIterations) {
            // Last iteration never runs.
            invocation.skip();
        } else {
            // Otherwise:
            // (A) Conditionally mark teardown of the previous iteration complete, if not done yet.
            // (B) Ask the server if we have a test iteration to run and run it if so.
            if (Helpers.shouldBypassExecution(webClient, currentIteration, "testTemplate")) {
                invocation.skip();
            } else {
                Helpers.proceedAndLogException(invocation, currentIteration, webClient);
            }
        }

        Property.setInstrumentationEnabledProperty(false);

        // Terminate on the last iteration.
        if ((currentIteration == maxIterations)) {
            FilibusterSystemProperties.unsetSystemPropertiesForFilibusterInstrumentation();
            FilibusterServerAPI.terminate(webClient);

            if (shouldInitializeFilibusterServer) {
                FilibusterServerLifecycle.stopServer(filibusterConfiguration, webClient);
            }
        }
    }

    @Override
    public void interceptBeforeEachMethod(InvocationInterceptor.Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext,
                                          ExtensionContext extensionContext) throws Throwable {
        if (currentIteration == 1) {
            // First iteration always runs because it's the fault free execution.
            invocation.proceed();
        } else if (currentIteration == maxIterations) {
            // Last iteration never runs.
            invocation.skip();
        } else {
            // Otherwise:
            // (A) Conditionally mark teardown of the previous iteration complete, if not done yet.
            // (B) Ask the server if we have a test iteration to run and run it if so.
            Helpers.conditionallyMarkTeardownComplete(invocationCompletionMap, currentIteration, webClient);

            if (Helpers.shouldBypassExecution(webClient, currentIteration, "beforeEach")) {
                invocation.skip();
            } else {
                invocation.proceed();
            }
        }
    }

    @Override
    public void interceptAfterEachMethod(InvocationInterceptor.Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext,
                                         ExtensionContext extensionContext) throws Throwable {
        if (currentIteration == 1) {
            // First iteration always runs because it's the fault free execution.
            invocation.proceed();
        } else if (currentIteration == maxIterations) {
            // Last iteration never runs.
            invocation.skip();
        } else {
            // Otherwise:
            // (A) Ask the server if we have a test iteration to run and run it if so.
            if (Helpers.shouldBypassExecution(webClient, currentIteration, "afterEach")) {
                invocation.skip();
            } else {
                invocation.proceed();
            }
        }
    }
}
