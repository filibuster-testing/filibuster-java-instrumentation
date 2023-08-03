package cloud.filibuster.junit.interceptors;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultNotInjectedException;
import cloud.filibuster.exceptions.filibuster.FilibusterOrganicFailuresPresentException;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.exceptions.filibuster.FilibusterNoopException;
import cloud.filibuster.junit.server.FilibusterServerAPI;
import cloud.filibuster.junit.server.core.FilibusterCore;
import com.linecorp.armeria.client.WebClient;
import org.junit.jupiter.api.extension.InvocationInterceptor;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

public class FilibusterInvocationInterceptorHelpers {
    private static final Logger logger = Logger.getLogger(FilibusterInvocationInterceptorHelpers.class.getName());

    private FilibusterInvocationInterceptorHelpers() {

    }

    @SuppressWarnings("InterruptedExceptionSwallowed")
    public static boolean shouldBypassExecution(WebClient webClient, int currentIteration, String caller, boolean abortOnFirstFailure, boolean previousExecutionFailed) {
        try {
            boolean hasNextIteration = FilibusterServerAPI.hasNextIteration(webClient, currentIteration, caller);

            if (hasNextIteration) {
                if (abortOnFirstFailure && previousExecutionFailed) {
                    return true;
                }

                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            throw new FilibusterRuntimeException("Filibuster server threw: " + e, e);
        }
    }

    public static void proceedAndLogException(FilibusterInvocationInterceptor filibusterInvocationInterceptor,
                                              InvocationInterceptor.Invocation<Void> invocation,
                                              int currentIteration,
                                              WebClient webClient,
                                              FilibusterConfiguration filibusterConfiguration) throws Throwable {
        proceedAndLogException(filibusterInvocationInterceptor, invocation, currentIteration, webClient, filibusterConfiguration,/* shouldWritePlaceholder= */true,/* shouldPrintRPCSummary= */true);
    }

    @SuppressWarnings("InterruptedExceptionSwallowed")
    public static void proceedAndLogException(FilibusterInvocationInterceptor filibusterInvocationInterceptor,
                                              InvocationInterceptor.Invocation<Void> invocation,
                                              int currentIteration,
                                              WebClient webClient,
                                              FilibusterConfiguration filibusterConfiguration,
                                              boolean shouldWritePlaceholder,
                                              boolean shouldPrintRPCSummary) throws Throwable {
        try {
            if (getServerBackendCanInvokeDirectlyProperty()) {
                if (FilibusterCore.hasCurrentInstance() && shouldWritePlaceholder) {
                    FilibusterCore.getCurrentInstance().writePlaceholderReport();
                }
            }

            invocation.proceed();

            if (filibusterConfiguration.getFailOnOrganicFailures() && FilibusterCore.hasCurrentInstance() && FilibusterCore.getCurrentInstance().testContainsOrganicFailures()) {
                FilibusterOrganicFailuresPresentException t = new FilibusterOrganicFailuresPresentException("Organic failures present: did you stubs for all invoked RPCs?");
                FilibusterServerAPI.recordIterationComplete(webClient, currentIteration, /* exceptionOccurred= */ true, t, shouldPrintRPCSummary);
                FilibusterInvocationInterceptor.previousIterationFailed = true;
                throw t;
            } else {
                FilibusterServerAPI.recordIterationComplete(webClient, currentIteration, /* exceptionOccurred= */ false, null, shouldPrintRPCSummary);
            }
        } catch (Throwable t) {
            Class<? extends Throwable> expectedExceptionClass = filibusterConfiguration.getExpected();

            if (expectedExceptionClass != FilibusterNoopException.class && expectedExceptionClass.isInstance(t)) {
                // FilibusterFaultNotInjectedException is thrown by recordIterationComplete -> FilibusterCore.completeIteration
                // In this case, we do not need to call recordIterationComplete again since invocation has already been recorded
                if (!expectedExceptionClass.equals(FilibusterFaultNotInjectedException.class)) {
                    FilibusterServerAPI.recordIterationComplete(webClient, currentIteration, /* exceptionOccurred= */false, null, shouldPrintRPCSummary);
                }
            } else {
                FilibusterServerAPI.recordIterationComplete(webClient, currentIteration, /* exceptionOccurred= */true, t, shouldPrintRPCSummary);
                FilibusterInvocationInterceptor.previousIterationFailed = true;
                throw t;
            }
        }
    }

    /**
     * Conditionally mark the teardown for a given test iteration complete.
     * <p>
     * There is a significant amount of nuance in this function.  The Filibuster server needs to know when a particular
     * test iteration is done and all the teardown is complete.  This is non-trivial because test functions might contain
     * and arbitrary number of afterEach methods (or, none at all.)  Therefore, the only way to ensure that we capture this is to do the following.
     * <p>
     * First, instrument the beforeEach and the testTemplate method.  Record a boolean in a map to indicate that the previous test is
     * complete when we reach the start of a new test.  However, some tests might have a beforeEach executed in the first iteration,
     * therefore, we also need to prevent notifying the server on the first iteration (iteration 0, the start of the first actual test.)
     *
     * @param invocationCompletionMap tracks the invocations that have completed and all teardowns have finished.
     * @param currentIteration        the iteration we are currently in, not the iteration that's been completed (current - 1).
     * @param webClient               a web client to use to talk to the Filibuster Server.
     */
    public static void conditionallyMarkTeardownComplete(HashMap<Integer, Boolean> invocationCompletionMap, int currentIteration, WebClient webClient) {
        int previousIteration = currentIteration - 1;

        if (!invocationCompletionMap.containsKey(previousIteration) && (previousIteration != 0)) {
            try {
                FilibusterServerAPI.teardownsCompleted(webClient, previousIteration);
            } catch (ExecutionException | InterruptedException e) {
                logger.log(Level.SEVERE, "Could not notify Filibuster of teardown completed; this is fatal error: " + e);
            }
            invocationCompletionMap.put(previousIteration, true);
        }
    }
}
