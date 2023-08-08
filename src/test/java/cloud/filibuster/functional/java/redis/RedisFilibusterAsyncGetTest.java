package cloud.filibuster.functional.java.redis;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisExhaustiveAnalysisConfigurationFile;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisFilibusterAsyncGetTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    private static final List<String> allowedExceptionMessages = new ArrayList<>();

    @BeforeAll
    public static void beforeAll() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
    }

    static {
        allowedExceptionMessages.add("Command timed out after 100 millisecond(s)");
    }

    @DisplayName("Tests whether Redis async interceptor can read from existing key - Single fault injection")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisExhaustiveAnalysisConfigurationFile.class,
            suppressCombinations = true)
    public void testRedisAsyncGet() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
            RedisAsyncCommands<String, String> myRedisCommands = myStatefulRedisConnection.async();

            myRedisCommands.set(key, value);

            RedisFuture<String> returnFuture = myRedisCommands.get(key);

            String returnVal = returnFuture.get();

            assertEquals(value, returnVal);

            if (wasFaultInjected()) {
                // In this test, the set method is async. The fault injected on the async set should only reflect on a potential Future.get.
                // Since Future.get is not called on the set method, the injected exception is swallowed and the catch block is not invoked.
                assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.async.RedisStringAsyncCommands/set"));
            }
        } catch (@SuppressWarnings("InterruptedExceptionSwallowed") Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("io.lettuce.core.api.async.RedisStringAsyncCommands/get"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.async.RedisStringAsyncCommands/get"), "Fault was not injected on the expected Redis method: " + t);
            assertTrue(t instanceof RedisCommandTimeoutException, "Fault was not of the correct type: " + t);
            assertTrue(allowedExceptionMessages.contains(t.getMessage()), "Unexpected fault message: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(3, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(1, testExceptionsThrown.size());
    }

}
