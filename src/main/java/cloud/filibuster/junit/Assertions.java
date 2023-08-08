package cloud.filibuster.junit;

import cloud.filibuster.exceptions.filibuster.FilibusterAllowedTimeExceededException;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedByHTTPServerException;
import cloud.filibuster.junit.server.core.FilibusterCore;
import org.junit.jupiter.api.function.ThrowingConsumer;

import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjectedHelper;

/**
 * Assertions provided by Filibuster for writing conditional, fault-based assertions.
 */
public class Assertions {

    /**
     * Asserts the fault-free execution passes and that the fault executions pass or throw a given exception.
     *
     * @param milliseconds time the passing executions must be executed within.
     * @param throwable class of exception thrown whenever an exception is thrown.
     * @param testBlock block containing the test code to execute.
     */
    public static void assertPassesWithinMsOrThrowsUnderFault(int milliseconds, Class<? extends Throwable> throwable, Runnable testBlock) {
        try {
            long startTime = System.nanoTime();
            testBlock.run();
            long endTime = System.nanoTime();

            long duration = (endTime - startTime);
            long durationMs = duration / 1000000;
            if (durationMs > milliseconds) {
                throw new FilibusterAllowedTimeExceededException("Test completed in " + durationMs +" milliseconds, exceeding allowed " + milliseconds + " milliseconds.");
            }
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
     * @param milliseconds time the passing executions must be executed within.
     * @param throwable class of exception thrown whenever an exception is thrown.
     * @param testBlock block containing the test code to execute.
     * @param assertionBlock block containing the conditional assertions to execute (throws, takes one parameter containing the @Throwable.)
     */
    public static void assertPassesWithinMsOrThrowsUnderFault(int milliseconds, Class<? extends Throwable> throwable, Runnable testBlock, ThrowingConsumer<Throwable> assertionBlock) throws Throwable {
        try {
            long startTime = System.nanoTime();
            testBlock.run();
            long endTime = System.nanoTime();

            long duration = (endTime - startTime);
            long durationMs = duration / 1000000;
            if (durationMs > milliseconds) {
                throw new FilibusterAllowedTimeExceededException("Test completed in " + durationMs +" milliseconds, exceeding allowed " + milliseconds + " milliseconds.");
            }
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
     * Determine if a fault was injected during the current test execution for a particular request.
     *
     * @param serializedRequest the @toString of the request.
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnRequest(String serializedRequest) {
        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                return FilibusterCore.getCurrentInstance().wasFaultInjectedOnRequest(serializedRequest);
            } else {
                return false;
            }
        } else {
            throw new FilibusterUnsupportedByHTTPServerException("wasFaultInjectedOnRequest only supported with local server.");
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

    /**
     * Determine if a fault was injected during the current test execution.
     *
     * @param fullyQualifiedMethodName fully qualified RPC method name (e.g., cloud.filibuster.WorldService/World)
     * @return was fault injected
     */
    public static boolean wasFaultInjectedOnMethod(String fullyQualifiedMethodName) {
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
}
