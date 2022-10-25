package cloud.filibuster.junit.interceptors;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.exceptions.FilibusterServerUnavailabilityException;
import cloud.filibuster.instrumentation.helpers.Property;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerAPI;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invocation Interceptor for automatically running tests with Filibuster.
 */
public class FilibusterInvocationInterceptor implements InvocationInterceptor {
    private static final Logger logger = Logger.getLogger(FilibusterInvocationInterceptor.class.getName());

    public static boolean shouldInitializeFilibusterServer = true;

    private final ProcessBuilder filibusterServerProcessBuilder;

    private final FilibusterConfiguration filibusterConfiguration;

    private final HashMap<Integer, Boolean> invocationCompletionMap;

    @Nullable
    private static Process filibusterServerProcess;

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

        this.filibusterServerProcessBuilder = new ProcessBuilder()
                .command(filibusterConfiguration.toExecutableCommand());

        this.webClient = WebClient.builder(filibusterConfiguration.getFilibusterBaseUri())
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .build();
    }

    /****************************************************************************************************************
     * Filibuster lifecycle.
     */

    static class FilibusterServerLifecycle {
        private static synchronized void startServer(ProcessBuilder filibusterServerProcessBuilder, WebClient webClient) throws InterruptedException, IOException {
            if (filibusterServerProcess == null) {
                filibusterServerProcess = filibusterServerProcessBuilder.start();

                boolean online = false;

                for (int i = 0; i < 10; i++) {
                    logger.log(Level.INFO, "Waiting for FilibusterServer to come online...");

                    try {
                        // Get remote resource.
                        RequestHeaders getHeaders = RequestHeaders.of(
                                HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                        // Get headers and verify a 200 OK response.
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);

                        if (Objects.equals(statusCode, "200")) {
                            logger.log(Level.INFO, "Available!");
                            online = true;
                            break;
                        } else {
                            logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                        }
                    } catch (RuntimeException e) {
                        // Nothing, we'll try again.
                    }

                    logger.log(Level.INFO, "Sleeping one second...");
                    Thread.sleep(1000);
                }

                if (!online) {
                    logger.log(Level.INFO, "FilibusterServer never came online!");
                    throw new FilibusterServerUnavailabilityException();
                }
            }
        }

        @SuppressWarnings("BusyWait")
        private static synchronized void stopServer(WebClient webClient) throws InterruptedException {
            if (filibusterServerProcess != null) {
                filibusterServerProcess.destroyForcibly();

                logger.log(Level.WARNING, "Waiting for Filibuster server to exit.");
                int exitCode = filibusterServerProcess.waitFor();
                logger.log(Level.WARNING, "Exit code for Filibuster:: " + exitCode);

                while (true) {
                    logger.log(Level.INFO, "Waiting for FilibusterServer to stop...");

                    try {
                        // Get remote resource.
                        RequestHeaders getHeaders = RequestHeaders.of(
                                HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                        // Get headers and verify a 200 OK response.
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);

                        if (Objects.equals(statusCode, "200")) {
                            logger.log(Level.INFO, "Still available!");
                        } else {
                            logger.log(Level.INFO, "Status code: " + statusCode);
                        }
                    } catch (RuntimeException e) {
                        break;
                    }

                    logger.log(Level.INFO, "Sleeping one second until offline.");
                    Thread.sleep(1000);
                }

                logger.log(Level.INFO, "Filibuster server stopped!");

                filibusterServerProcess = null;
            }
        }
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
                FilibusterServerLifecycle.startServer(filibusterServerProcessBuilder, webClient);
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
                FilibusterServerLifecycle.stopServer(webClient);
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
