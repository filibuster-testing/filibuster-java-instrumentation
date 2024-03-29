package cloud.filibuster.functional.database.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTransformStringAnalysisConfigurationFile;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisFilibusterStringTransformerTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(key, value);
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - String transformer faults.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformStringAnalysisConfigurationFile.class)
    public void testRedisStringTransformation() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();
            String returnVal = myRedisCommands.get(key);
            assertEquals(value, returnVal);
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/get"), "Fault was not injected on the expected Redis method: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // 1 for the original test and +1 for each character manipulation in the string
    public void testNumExecutions() {
        assertEquals(value.length() + 1, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(value.length(), testExceptionsThrown.size());
    }

}
