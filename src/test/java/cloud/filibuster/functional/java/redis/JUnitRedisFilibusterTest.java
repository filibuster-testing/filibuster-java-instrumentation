package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.LettuceInterceptedConnection;
import cloud.filibuster.instrumentation.libraries.lettuce.LettuceInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import cloud.filibuster.junit.TestWithFilibuster;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterTest extends JUnitAnnotationBaseTest {
    private static StatefulRedisConnection<String, String> redisConnection;
    String key = "test";
    String value = "example";

    @BeforeAll
    public static void setUp() {
        redisConnection = RedisConnection.getInstance().connection;
    }

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
    }

    @AfterEach
    public void afterEach() {
        LettuceInterceptor.isFaultInjected = false;
    }

    private <T> T getRedisConnection(Class<T> type, boolean isFaultInjected) {
        if (isFaultInjected) {
            LettuceInterceptor.isFaultInjected = true;
            return LettuceInterceptedConnection.create(redisConnection, type);
        }
        if (type == RedisCommands.class)
            return type.cast(redisConnection.sync());
        if (type == RedisAsyncCommands.class)
            return type.cast(redisConnection.async());
        if (type == RedisReactiveCommands.class)
            return type.cast(redisConnection.reactive());
        throw new IllegalArgumentException("Unknown type");
    }

    @Test
    @DisplayName("Tests whether Redis sync interceptor can inject a timeout exception")
    @Order(1)
    @TestWithFilibuster(maxIterations = 1)
    public void testRedisSyncException() {
        RedisCommands<String, String> myRedisCommands = getRedisConnection(RedisCommands.class, true);
        assertThrows(RedisCommandTimeoutException.class, () -> myRedisCommands.set(key, value),
                "An exception was thrown at LettuceInterceptor");
    }

    @Test
    @DisplayName("Tests whether Redis sync interceptor connection can read and write")
    @Order(2)
    public void testRedisSync() {
        RedisCommands<String, String> myRedisCommands = getRedisConnection(RedisCommands.class, false);
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }

}
