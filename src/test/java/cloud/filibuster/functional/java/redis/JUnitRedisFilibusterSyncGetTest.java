package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
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

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterSyncGetTest extends JUnitAnnotationBaseTest {
    static String key = "test";
    static String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    private static final List<String> allowedExceptionMessages = new ArrayList<>();

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(key, value);
    }

    static {
        allowedExceptionMessages.add("Command timed out after 100 millisecond(s)");
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Single fault injection")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile.class)
    public void testRedisSyncGet() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();
            String returnVal = myRedisCommands.get(key);
            assertEquals(value, returnVal);
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                testExceptionsThrown.add(t.getMessage());
                assertTrue(wasFaultInjectedOnService(REDIS_MODULE_NAME),
                        "Fault was not injected on the Redis module.");
                assertTrue(t instanceof RedisCommandTimeoutException,
                        "Fault was not of the correct type");
                assertTrue(allowedExceptionMessages.contains(t.getMessage()),
                        "Unexpected fault message");
                return;
            }
            throw t;
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(2, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(1, testExceptionsThrown.size());
    }

}
