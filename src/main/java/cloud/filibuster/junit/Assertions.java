package cloud.filibuster.junit;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.junit.exceptions.InternalAssertionFailureException;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Assertions provided by Filibuster for writing conditional, fault-based assertions.
 */
public class Assertions {
    private static final Logger logger = Logger.getLogger(Assertions.class.getName());

    private static String getFilibusterBaseUri() {
        return "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";
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
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        try {
            // Get headers and verify a 200 OK response.
            ResponseHeaders headers = response.headers();
            String statusCode = headers.get(HttpHeaderNames.STATUS);

            if (statusCode == null) {
                logger.log(Level.SEVERE, "wasFaultInjected, statusCode: null");
                throw new InternalAssertionFailureException();
            }

            if (!statusCode.equals("200")) {
                logger.log(Level.SEVERE, "wasFaultInjected, statusCode: " + statusCode);
                throw new InternalAssertionFailureException();
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
        return wasFaultInjected("/filibuster/fault-injected/service/" + serviceName);
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
        return wasFaultInjected("/filibuster/fault-injected/method/" + serviceName + "/" + methodName);
    }

    /**
     * Determine if a fault was injected during the current test execution.
     *
     * @param fullyQualifiedMethodName fully qualified RPC method name (e.g., cloud.filibuster.WorldService/World)
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnMethod(String fullyQualifiedMethodName) {
        return wasFaultInjected("/filibuster/fault-injected/method/" + fullyQualifiedMethodName);
    }
}
