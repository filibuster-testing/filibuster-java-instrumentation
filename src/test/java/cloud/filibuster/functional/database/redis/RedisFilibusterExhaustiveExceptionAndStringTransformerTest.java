package cloud.filibuster.functional.database.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisExhaustiveExceptionAndTransformerAnalysisConfigurationFile;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisFilibusterExhaustiveExceptionAndStringTransformerTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void beforeAll() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Exhaustive Exception and String transformer faults.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisExhaustiveExceptionAndTransformerAnalysisConfigurationFile.class)
    public void testRedisStringBFIAndExceptionInjection() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

            myRedisCommands.set(key, value);
            String returnVal = myRedisCommands.get(key);
            assertEquals(value, returnVal);

            myRedisCommands.set(value, key);
            returnVal = myRedisCommands.get(value);
            assertEquals(key, returnVal);

            assertNull(myRedisCommands.get("NonexistentKey"));

            myRedisCommands.set(key, "");
            returnVal = myRedisCommands.get(key);
            assertEquals("", returnVal);

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/get") ||
                            wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/set"),
                    "Fault was not injected on the expected Redis method: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // Reference execution + 3 RedisCommandTimeoutExceptions injected on set + 4 RedisCommandTimeoutExceptions injected on get
        // + transformer faults on "example" (7 chars) + transformer faults on "test" (4 chars)
        assertEquals(19, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of unique injected faults.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        // RedisCommandTimeoutException injected on set/get
        // + transformer faults on "example" (7 chars) + transformer faults on "test" (4 chars)
        assertEquals(12, testExceptionsThrown.size());
    }

}
