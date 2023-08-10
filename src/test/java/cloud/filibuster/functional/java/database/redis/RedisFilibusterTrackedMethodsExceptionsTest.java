package cloud.filibuster.functional.java.database.redis;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultNotInjectedAndATrackedMethodInvokedException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTrackedFunctionAnalysisConfigurationFile;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisFilibusterTrackedMethodsExceptionsTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;

    static String redisConnectionString;
    private static int numberOfTestExecutions = 0;
    // Map in the format <ExceptionClass -> (<ClassNameOfMethod -> List(MethodName)>, ExceptionMessage)>.
    // The ClassNameOfMethod and MethodName are used to check if the fault was injected in the correct method and service.
    private static final Map<Class<?>, Entry<Map<String, List<String>>, String>> allowedExceptions = new HashMap<>();

    @BeforeAll
    public static void primeCacheBeforeAll() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
    }

    @BeforeEach
    public void primeCacheBeforeEach() {
        statefulRedisConnection.sync().set(key, value);
    }

    @AfterEach
    public void primeCacheAfterEach() {
        statefulRedisConnection.sync().flushall();
    }

    static {
        allowedExceptions.put(RedisCommandTimeoutException.class,
                new SimpleEntry<>(ImmutableMap.of(
                        "io.lettuce.core.api.async.RedisStringAsyncCommands", Collections.singletonList("set")), "Command timed out after 100 millisecond(s)"));
    }

    @DisplayName("Fail Filibuster test if fault not injected and a tracked method is invoked")
    @Order(1)
    @TestWithFilibuster(
            analysisConfigurationFile = RedisTrackedFunctionAnalysisConfigurationFile.class,
            suppressCombinations = true,
            failIfFaultNotInjectedAndATrackedMethodIsInvoked = true,
            expected = FilibusterFaultNotInjectedAndATrackedMethodInvokedException.class)
    public void testRedisFaultNotInjectedAndATrackedMethodInvoked() {
        try {
            numberOfTestExecutions++;

            RedisAsyncCommands<String, String> redisAsyncCommands = statefulRedisConnection.async();
            RedisAsyncCommands<String, String> myRedisAsyncCommands = DynamicProxyInterceptor.createInterceptor(redisAsyncCommands, redisConnectionString);

            RedisFuture<String> future = myRedisAsyncCommands.set(key, value);

            future.thenAccept((s) -> {
                // Do nothing
            });

        } catch (@SuppressWarnings("InterruptedExceptionSwallowed") Throwable t) {

            // Assert that a fault was injected
            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);

            Entry<Map<String, List<String>>, String> classMethodsMessageTuple = allowedExceptions.get(t.getClass());

            if (classMethodsMessageTuple != null) {
                String expectedExceptionMessage = classMethodsMessageTuple.getValue();
                // Assert that the exception message matches the expected one
                assertEquals(expectedExceptionMessage, t.getMessage(), "Unexpected fault message: " + t);

                Map<String, List<String>> classMethodsMap = classMethodsMessageTuple.getKey();
                boolean injectedMethodFound = false;

                for (Entry<String, List<String>> mapEntry : classMethodsMap.entrySet()) {
                    String className = mapEntry.getKey();
                    List<String> methodNames = mapEntry.getValue();

                    if (methodNames.stream().anyMatch(method -> wasFaultInjectedOnJavaClassAndMethod(className + "/" + method))) {
                        injectedMethodFound = true;
                        break;
                    }
                }

                // Assert that the fault was injected on one of the expected methods of the given class
                if (!injectedMethodFound) {
                    throw new AssertionFailedError("Fault was not injected on any of the expected methods: " + t);
                }
            } else {
                throw new AssertionFailedError("Injected fault was not defined for this test: " + t);
            }
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // Reference execution + 1 fault injection
        assertEquals(2, numberOfTestExecutions);
    }

}
