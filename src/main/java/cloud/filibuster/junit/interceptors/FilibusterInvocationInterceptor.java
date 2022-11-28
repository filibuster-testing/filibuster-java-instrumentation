package cloud.filibuster.junit.interceptors;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.exceptions.MissingWebClientException;
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
    private static WebClient privateWebClient;

    private static boolean hasWebClient() {
        return privateWebClient != null;
    }

    @Nullable
    public static WebClient getWebClient() throws Throwable {
        if (privateWebClient == null) {
            if (FilibusterServerLifecycle.didServerInitializationFail()) {
                throw FilibusterServerLifecycle.getInitializationFailedException();
            } else {
                throw new MissingWebClientException();
            }
        }

        return privateWebClient;
    }

    private static void setWebClient(@Nullable WebClient webClient) {
        privateWebClient = webClient;
    }

    private static WebClient getNewWebClient() {
        String filibusterBaseUri = "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";

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
    public void interceptTestMethod(InvocationInterceptor.Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        Property.setInstrumentationEnabledProperty(true);

        // Test handling.
        if (currentIteration == 1) {
            // First iteration always runs because it's the fault free execution.
            FilibusterInvocationInterceptorHelpers.proceedAndLogException(invocation, currentIteration, getWebClient());
        } else if (currentIteration == maxIterations) {
            // Last iteration never runs.
            invocation.skip();
        } else {
            // Otherwise:
            // (A) Conditionally mark teardown of the previous iteration complete, if not done yet.
            // (B) Ask the server if we have a test iteration to run and run it if so.
            if (FilibusterInvocationInterceptorHelpers.shouldBypassExecution(getWebClient(), currentIteration, "testTemplate")) {
                invocation.skip();
            } else {
                FilibusterInvocationInterceptorHelpers.proceedAndLogException(invocation, currentIteration, getWebClient());
            }
        }

        Property.setInstrumentationEnabledProperty(false);
    }

    @Override
    public void interceptTestTemplateMethod(InvocationInterceptor.Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {

        // **************************************************************************************************
        // 1. Start handling specific to the first iteration.
        // **************************************************************************************************

        if (currentIteration == 1) {

            // First iteration always needs to start the server unless we are reusing an external server process.
            if (shouldInitializeFilibusterServer) {
                setWebClient(FilibusterServerLifecycle.startServer(filibusterConfiguration));
            } else {
                setWebClient(getNewWebClient());
            }

            // Remotely load analysis file configuration in for fault selection.
            FilibusterServerAPI.analysisFile(getWebClient(), filibusterConfiguration.readAnalysisFile());

            // Configure DEI algorithm and enable Kotlin debugging.
            FilibusterSystemProperties.setSystemPropertiesForFilibusterInstrumentation(filibusterConfiguration);

        }

        // **************************************************************************************************
        // 2. Start handling specific to the first iteration.
        // **************************************************************************************************

        // Conditionally mark the last test as completed.
        //
        // This is necessary when the previous test finished, but we didn't have a beforeEach or afterEach
        // for the previous test (or this test), and we enter this function without having marked
        // the previous iteration complete.
        //
        // Therefore, mark it finished before we start this iteration.
        FilibusterInvocationInterceptorHelpers.conditionallyMarkTeardownComplete(invocationCompletionMap, currentIteration, getWebClient());

        // Invoke the test.
        interceptTestMethod(invocation, invocationContext, extensionContext);

        // **************************************************************************************************
        // 3. Start handling specific to the last iteration.
        // **************************************************************************************************

        if ((currentIteration == maxIterations)) {

            // Reset debugging system properties and DEI algorithm configuration.
            FilibusterSystemProperties.unsetSystemPropertiesForFilibusterInstrumentation();

            // Terminate the Filibuster server.
            FilibusterServerAPI.terminate(getWebClient());

            // Last iteration always needs to stop the server unless we are reusing an external server process.
            if (shouldInitializeFilibusterServer) {
                setWebClient(FilibusterServerLifecycle.stopServer(filibusterConfiguration, getWebClient()));
            } else {
                setWebClient(null);
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
            FilibusterInvocationInterceptorHelpers.conditionallyMarkTeardownComplete(invocationCompletionMap, currentIteration, getWebClient());

            if (FilibusterInvocationInterceptorHelpers.shouldBypassExecution(getWebClient(), currentIteration, "beforeEach")) {
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
            if (FilibusterInvocationInterceptorHelpers.shouldBypassExecution(getWebClient(), currentIteration, "afterEach")) {
                invocation.skip();
            } else {
                invocation.proceed();
            }
        }
    }
}
