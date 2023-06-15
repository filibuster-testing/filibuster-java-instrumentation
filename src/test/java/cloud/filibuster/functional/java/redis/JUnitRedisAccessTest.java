package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {
    final String key = "test";
    final String value = "example";
    static StatefulRedisConnection<String, String> redisConnection;
    static String connectionString;

    @BeforeAll
    public static void beforeAll() {
        redisConnection = RedisClientService.getInstance().redisClient.connect();
        connectionString = RedisClientService.getInstance().connectionString;
    }

    @Test
    @DisplayName("Tests whether Redis sync interceptor connection can read and write")
    @Order(1)
    public void testRedisSync() {
        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(redisConnection, connectionString);
        RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }

    @Test
    @DisplayName("Tests whether Redis async interceptor connection can read and write")
    @Order(2)
    public void testRedisAsync() throws ExecutionException, InterruptedException {
        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(redisConnection, connectionString);
        RedisAsyncCommands<String, String> myRedisCommands = myStatefulRedisConnection.async();
        myRedisCommands.set(key, value).get();
        String retrievedValue = myRedisCommands.get(key).get();
        assertEquals(value, retrievedValue);
    }

    @Test
    @DisplayName("Tests whether Redis reactive interceptor connection can read and write")
    @Order(3)
    public void testRedisReactive() {
        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(redisConnection, connectionString);
        RedisReactiveCommands<String, String> myRedisCommands = myStatefulRedisConnection.reactive();
        Mono<String> set = myRedisCommands.set(key, value);
        Mono<String> get = myRedisCommands.get(key);
        set.subscribe();
        assertEquals(value, get.block());
    }

}
