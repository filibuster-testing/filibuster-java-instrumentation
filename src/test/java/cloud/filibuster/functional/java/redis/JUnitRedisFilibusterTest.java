package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.RedisSingleFaultRuntimeExceptionAnalysisConfigurationFile;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @DisplayName("Tests whether Redis sync interceptor can write")
    @Order(1)
    @TestWithFilibuster(maxIterations = 2, analysisConfigurationFile = RedisSingleFaultRuntimeExceptionAnalysisConfigurationFile.class)
    public void testRedisSyncGet() {
        RedisCommands<String, String> myRedisCommands = new RedisInterceptorFactory(redisClient, redisConnectionString)
                .getProxy(RedisCommands.class);
        String returnVal = myRedisCommands.get(key);
        assertEquals(value, returnVal);
    }

}
