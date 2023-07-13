package cloud.filibuster.functional.shouldFails;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisExhaustiveAnalysisConfigurationFile;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FilibusterRedisAsyncSetFailTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void beforeAll() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
    }

    @DisplayName("This test should fail: Tests whether Redis async interceptor can set a value for a key")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisExhaustiveAnalysisConfigurationFile.class,
            failIfFaultNotInjected = true)
    public void testRedisAsyncSet() {
        numberOfTestExecutions++;

        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
        RedisAsyncCommands<String, String> myRedisCommands = myStatefulRedisConnection.async();

        myRedisCommands.set(key, value);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(2, numberOfTestExecutions);
    }

}
