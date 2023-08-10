package cloud.filibuster.junit.assertions.protocols;

import cloud.filibuster.junit.server.core.FilibusterCore;

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

    // TODO: add javadoc once implemented
    public static boolean wasFaultInjectedOnMethod(String httpMethod) {
        throw new HttpAssertionsNotImplementedException();
    }

    // TODO: add javadoc once implemented
    public static boolean wasFaultInjectedOnMethod(String httpMethod, String URI) {
        throw new HttpAssertionsNotImplementedException();
    }

    // TODO: add javadoc once implemented
    public static boolean wasFaultInjectedOnRequest(String request) {
        throw new HttpAssertionsNotImplementedException();
    }
}