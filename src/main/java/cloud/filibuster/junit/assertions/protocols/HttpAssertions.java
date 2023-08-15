package cloud.filibuster.junit.assertions.protocols;

import cloud.filibuster.junit.server.core.FilibusterCore;
import com.linecorp.armeria.common.HttpMethod;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedHelper;

public class HttpAssertions {

    public static class HttpAssertionsNotImplementedException extends RuntimeException {

    }

    /**
     * Returns true if a fault was injected on a HTTP service.
     *
     * <p>Requires that the invoked side uses the {@link cloud.filibuster.instrumentation.instrumentors.FilibusterServerInstrumentor FilibusterServerInstrumentor} in a {@link cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService FilibusterDecoratingHttpService}.</p>
     *
     * @param serviceName string service name
     * @return if a fault was injected
     */
    public static boolean wasFaultInjectedOnService(String serviceName) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnService(serviceName);
            } else {
                return false;
            }
        } else {
            return wasFaultInjectedHelper("/filibuster/fault-injected/service/" + serviceName);
        }
    }

    /**
     * Returns true if a fault was injected for a particular HTTP method.
     *
     * <p>This is most commonly represented by a combination of the request URI and verb.</p>
     *
     * @param httpMethod HTTP verb (a la Method)
     * @param uriPattern request URI {@link java.util.regex.Pattern}
     * @return if a fault was injected
     */
    public static boolean wasFaultInjectedOnHttpMethod(HttpMethod httpMethod, String uriPattern) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnHttpMethod(httpMethod, uriPattern);
            } else {
                return false;
            }
        } else {
            throw new HttpAssertionsNotImplementedException();
        }
    }

    /**
     * Returns true if a fault was injected for a particular HTTP method and request body.
     *
     * <p>This is most commonly represented by a combination of the request URI and verb.</p>
     *
     * @param httpMethod HTTP verb (a la Method)
     * @param uriPattern request URI {@link java.util.regex.Pattern}
     * @param serializedRequestPattern serialized request {@link java.util.regex.Pattern}
     * @return if a fault was injected
     */
    public static boolean wasFaultInjectedOnHttpRequest(HttpMethod httpMethod, String uriPattern, String serializedRequestPattern) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnHttpRequest(httpMethod, uriPattern, serializedRequestPattern);
            } else {
                return false;
            }
        } else {
            throw new HttpAssertionsNotImplementedException();
        }
    }
}
