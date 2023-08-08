package cloud.filibuster.junit;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedByHTTPServerException;
import cloud.filibuster.junit.server.core.FilibusterCore;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjectedHelper;

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
            throw new FilibusterUnsupportedAPIException("This API is currently not supported. If applicable, please import the GRPC variant of this method instead.");
        } else {
            return wasFaultInjectedHelper("/filibuster/fault-injected/service/" + serviceName);
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
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnMethod(serviceName, methodName);
            } else {
                return false;
            }
        } else {
            return wasFaultInjectedHelper("/filibuster/fault-injected/method/" + serviceName + "/" + methodName);
        }
    }
}
