package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterTest extends JUnitAnnotationBaseTest {
    static String key = "test";
    static String value = "example";
    static RedisClient redisClient;
    static String redisConnectionString;

    @BeforeAll
    public static void primeCache() {
        redisClient = RedisClientService.getInstance().redisClient;
        redisConnectionString = RedisClientService.getInstance().connectionString;
        redisClient.connect().sync().set(key, value);
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile.class)
    public void testRedisSyncGet() {
        try {
            RedisCommands<String, String> myRedisCommands = new RedisInterceptorFactory(redisClient, redisConnectionString).getProxy(RedisCommands.class);
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

    @DisplayName("Tests how Redis sync interceptor handles reading from non-existing key")
    @Order(2)
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleFaultCommandTimeoutExceptionAnalysisConfigurationFile.class)
    public void testRedisSyncGetNonExistingKey() {
        try {
            RedisCommands<String, String> myRedisCommands = new RedisInterceptorFactory(redisClient, redisConnectionString).getProxy(RedisCommands.class);
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
