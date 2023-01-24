package cloud.filibuster.junit;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.junit.exceptions.FilibusterServerBadResponseException;
import cloud.filibuster.junit.server.core.FilibusterCore;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

/**
 * Assertions provided by Filibuster for writing conditional, fault-based assertions.
 */
public class Assertions {
    private static final Logger logger = Logger.getLogger(Assertions.class.getName());

    private static String getFilibusterBaseUri() {
        return "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";
    }

    /**
     * Asserts the fault-free execution passes and that the fault executions pass or throw a given exception.
     *
     * @param throwable class of exception thrown whenever an exception is thrown.
     * @param testBlock block containing the test code to execute.
     */
    public static void assertPassesOrThrowsUnderFault(Class<? extends Throwable> throwable, Runnable testBlock) {
        try {
            testBlock.run();
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                if (!throwable.isInstance(t)) {
                    // Test threw, we didn't expect it: throw.
                    throw t;
                }

                // Test threw, we expected it: do nothing.
            } else {
                // Test threw, we didn't inject a fault: throw.
                throw t;
            }
        }
    }

    /**
     * Asserts the fault-free execution passes and that the fault executions pass or throw a given exception.
     *
     * @param throwable class of exception thrown whenever an exception is thrown.
     * @param testBlock block containing the test code to execute.
     * @param assertionBlock block containing the conditional assertions to execute (throws, takes one parameter containing the @Throwable.)
     */
    public static void assertPassesOrThrowsUnderFault(Class<? extends Throwable> throwable, Runnable testBlock, ThrowingConsumer<Throwable> assertionBlock) throws Throwable {
        try {
            testBlock.run();
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                if (!throwable.isInstance(t)) {
                    // Test threw, we didn't expect it: throw.
                    throw t;
                }

                // Test threw, we expected it: now check the conditional, user-provided, assertions.
                assertionBlock.accept(t);
            } else {
                // Test threw, we didn't inject a fault: throw.
                throw t;
            }
        }
    }

    /**
     * Determine if a fault was injected during the current test execution.
     *
     * @return was fault injected
     */
    public static boolean wasFaultInjected() {
        return wasFaultInjected("/filibuster/fault-injected");
    }

    private static boolean wasFaultInjected(String uri) {
        String filibusterBaseUri = getFilibusterBaseUri();
        WebClient webClient = FilibusterExecutor.getWebClient(filibusterBaseUri);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, uri, HttpHeaderNames.ACCEPT, "application/json");

        try {
            AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
            ResponseHeaders headers = response.headers();
            String statusCode = headers.get(HttpHeaderNames.STATUS);

            if (statusCode == null) {
                FilibusterServerBadResponseException.logAndThrow("wasFaultInjected, statusCode: null");
            }

            if (!Objects.equals(statusCode, "200")) {
                FilibusterServerBadResponseException.logAndThrow("wasFaultInjected, statusCode: " + statusCode);
            }

            // Get body and verify the proper response.
            JSONObject jsonObject = Response.aggregatedHttpResponseToJsonObject(response);
            return jsonObject.getBoolean("result");
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Couldn't connect to Filibuster server, assuming no fault was injected: " + e);
            return false;
        }
    }

    /**
     * Determine if a fault was injected during the current test execution.
     *
     * Only works if the target service is instrumented using a server instrumentor and reports its name.
     *
     * @param serviceName service name, as reported by a server instrumentor.
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnService(String serviceName) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            return FilibusterCore.getCurrentInstance().wasFaultInjectedOnService(serviceName);
        } else {
            return wasFaultInjected("/filibuster/fault-injected/service/" + serviceName);
        }
    }

    /**
     * Determine if a fault was injected during the current test execution.
     *
     * Does not require server instrumentor usage, as service name is determined by invoking stub if using stubs. (i.e., Google gRPC)
     *
     * @param serviceName service name (e.g., cloud.filibuster.WorldService)
     * @param methodName method name (e.g., World)
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnMethod(String serviceName, String methodName) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethod(serviceName, methodName);
        } else {
            return wasFaultInjected("/filibuster/fault-injected/method/" + serviceName + "/" + methodName);
        }
    }

    /**
     * Determine if a fault was injected during the current test execution.
     *
     * @param fullyQualifiedMethodName fully qualified RPC method name (e.g., cloud.filibuster.WorldService/World)
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnMethod(String fullyQualifiedMethodName) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            String[] split = fullyQualifiedMethodName.split("/", 2);
            return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethod(split[0], split[1]);
        } else {
            return wasFaultInjected("/filibuster/fault-injected/method/" + fullyQualifiedMethodName);
        }
    }
}
