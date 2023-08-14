package cloud.filibuster.junit.assertions.protocols;

import cloud.filibuster.junit.server.core.FilibusterCore;
import com.google.errorprone.annotations.DoNotCall;
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
     * @param URI request URI
     * @return if a fault was injected
     */
    // TODO: add javadoc once implemented
    public static boolean wasFaultInjectedOnMethod(HttpMethod httpMethod, String URI) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethod(httpMethod, URI);
            } else {
                return false;
            }
        } else {
            throw new HttpAssertionsNotImplementedException();
        }
    }

    // TODO: add javadoc once implemented
    @DoNotCall("Always throws cloud.filibuster.junit.assertions.protocols.HttpAssertions.HttpAssertionsNotImplementedException")
    public static boolean wasFaultInjectedOnRequest(String request) {
        throw new HttpAssertionsNotImplementedException();
    }
}
