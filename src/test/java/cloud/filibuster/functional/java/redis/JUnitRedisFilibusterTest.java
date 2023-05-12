package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.RedisDefaultAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.examples.RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import static cloud.filibuster.instrumentation.helpers.Property.setTestPortNondeterminismProperty;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterTest extends JUnitAnnotationBaseTest {
    static String key = "test";
    static String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(key, value);
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Single fault injection")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile.class)
    public void testRedisSyncGet() {
        try {
            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();
            String returnVal = myRedisCommands.get(key);
            assertEquals(value, returnVal);
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            if (wasFaultInjected() && t.getMessage().equals("Command timed out after 100 millisecond(s)")) {
                return;
            }
            throw t;
        }
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Multiple fault injections")
    @Order(2)
    @TestWithFilibuster(analysisConfigurationFile = RedisDefaultAnalysisConfigurationFile.class, portNondeterminism = true)
    public void testRedisSyncGetMultipleTests() {
        try {
            setTestPortNondeterminismProperty(true);
            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();
            String returnVal = myRedisCommands.get(key);
            assertEquals(value, returnVal);
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                if (t.getMessage().equals("Command timed out after 100 millisecond(s)") ||
                        t.getMessage().equals("Connection closed prematurely"))
                    return;
            }
            throw t;
        }
    }

    @DisplayName("Tests how Redis sync interceptor handles reading from non-existing key")
    @Order(3)
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile.class)
    public void testRedisSyncGetNonExistingKey() {
        try {
            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();
            String returnVal = myRedisCommands.get("ThisKeyDoesNotExist");
            assertNull(returnVal);
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            if (wasFaultInjected() && t.getMessage().equals("Command timed out after 100 millisecond(s)")) {
                return;
            }
            throw t;
        }
    }

}
