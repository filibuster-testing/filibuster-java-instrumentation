package cloud.filibuster.junit;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedByHTTPServerException;
import cloud.filibuster.junit.server.core.FilibusterCore;
import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;

/**
 * Assertions provided by Filibuster for writing conditional, fault-based assertions.
 */
public class Assertions {
    /**
     * Determine if a fault was injected during the current test execution for a particular request and method.
     *
     * @param fullyQualifiedMethodName grpc method in the format Service/Method
     * @param contains substring to search for
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnMethodWherePayloadContains(String fullyQualifiedMethodName, String contains) {
        String[] split = fullyQualifiedMethodName.split("/", 2);

        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethodWhereRequestContains(split[0], split[1], contains);
            } else {
                return false;
            }
        } else {
            throw new FilibusterUnsupportedByHTTPServerException("wasFaultInjectedOnMethodWherePayloadContains only supported with local server.");
        }
    }
}
