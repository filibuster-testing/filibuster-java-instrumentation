package cloud.filibuster.junit.assertions.protocols;

import cloud.filibuster.exceptions.filibuster.FilibusterServerBadResponseException;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.junit.server.core.FilibusterCore;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

public class GenericAssertions {
    /**
     * Returns true if any fault was injected.
     *
     * @return if a fault was injected.
     */
    public static boolean wasFaultInjected() {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjected();
            } else {
                return false;
            }
        } else {
            return wasFaultInjectedHelper("/filibuster/fault-injected");
        }
    }

    /**
     * Returns true if a fault was injected on the following Java method.
     *
     * @param className class name including fully-qualified namespace and package
     * @param methodName method name
     * @return if a fault was injected.
     */
    public static boolean wasFaultInjectedOnJavaClassAndMethod(String className, String methodName) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethod(className, methodName);
            } else {
                return false;
            }
        } else {
            return wasFaultInjectedHelper("/filibuster/fault-injected/method/" + className + "/" + methodName);
        }
    }


    /**
     * Returns true if a fault was injected on the following Java method.
     *
     * @param fullyQualifiedMethodName fully qualified class name in form com.namespace.package.class/Method*
     * @return if a fault was injected.
     */
    public static boolean wasFaultInjectedOnJavaClassAndMethod(String fullyQualifiedMethodName) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            String[] split = fullyQualifiedMethodName.split("/", 2);

            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethod(split[0], split[1]);
            } else {
                return false;
            }
        } else {
            return wasFaultInjectedHelper("/filibuster/fault-injected/method/" + fullyQualifiedMethodName);
        }
    }


    protected static boolean wasFaultInjectedHelper(String uri) {
        Logger logger = Logger.getLogger(GrpcAssertions.class.getName());
        String filibusterBaseUri = "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";
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
}
