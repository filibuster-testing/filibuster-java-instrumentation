package cloud.filibuster.junit.interceptors;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Property;
import cloud.filibuster.junit.FilibusterSystemProperties;
import cloud.filibuster.junit.server.FilibusterServerLifecycle;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerAPI;
import com.linecorp.armeria.client.WebClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
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

    @Nullable
    private static WebClient webClient;

    private final static WebClient getNewWebClient() {
        String filibusterBaseUri =  "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";

        return WebClient.builder(filibusterBaseUri)
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .build();
    }

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
                webClient = FilibusterServerLifecycle.startServer(filibusterConfiguration);
                FilibusterServerAPI.analysisFile(webClient, filibusterConfiguration.readAnalysisFile());
            } else {
                webClient = getNewWebClient();
            }
        }

        FilibusterInvocationInterceptorHelpers.conditionallyMarkTeardownComplete(invocationCompletionMap, currentIteration, webClient);

        Property.setInstrumentationEnabledProperty(true);

        // Test handling.
        if (currentIteration == 1) {
            // First iteration always runs because it's the fault free execution.
            FilibusterInvocationInterceptorHelpers.proceedAndLogException(invocation, currentIteration, webClient);
        } else if (currentIteration == maxIterations) {
            // Last iteration never runs.
            invocation.skip();
        } else {
            // Otherwise:
            // (A) Conditionally mark teardown of the previous iteration complete, if not done yet.
            // (B) Ask the server if we have a test iteration to run and run it if so.
            if (FilibusterInvocationInterceptorHelpers.shouldBypassExecution(webClient, currentIteration, "testTemplate")) {
                invocation.skip();
            } else {
                FilibusterInvocationInterceptorHelpers.proceedAndLogException(invocation, currentIteration, webClient);
            }
        }

        Property.setInstrumentationEnabledProperty(false);

        // Terminate on the last iteration.
        if ((currentIteration == maxIterations)) {
            FilibusterSystemProperties.unsetSystemPropertiesForFilibusterInstrumentation();
            FilibusterServerAPI.terminate(webClient);

            if (shouldInitializeFilibusterServer) {
                webClient = FilibusterServerLifecycle.stopServer(filibusterConfiguration, webClient);
            } else {
                webClient = null;
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
            FilibusterInvocationInterceptorHelpers.conditionallyMarkTeardownComplete(invocationCompletionMap, currentIteration, webClient);

            if (FilibusterInvocationInterceptorHelpers.shouldBypassExecution(webClient, currentIteration, "beforeEach")) {
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
            if (FilibusterInvocationInterceptorHelpers.shouldBypassExecution(webClient, currentIteration, "afterEach")) {
                invocation.skip();
            } else {
                invocation.proceed();
            }
        }
    }
}
