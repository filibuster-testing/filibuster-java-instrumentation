package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.lettuce.LettuceInterceptedConnection;
import cloud.filibuster.instrumentation.libraries.lettuce.LettuceInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {
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

    @Test
    @DisplayName("Tests reading data from Redis")
    @Order(1)
    public void testRedisHello() {
        ManagedChannel apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();

        // Prime the cache: Insert key-value-pair into Redis
        redisConnection.sync().set(key, value);


        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.RedisRequest readRequest = Hello.RedisRequest.newBuilder().setKey(key).build();
            Hello.RedisReply reply = blockingStub.redisHello(readRequest);
            assertEquals(value, reply.getValue());
        } catch (Throwable t) {
            if (wasFaultInjected() && t.getMessage().equals("io.grpc.StatusRuntimeException: INTERNAL: java.lang.Exception: An exception was thrown at Hello service")) {
                return;
            }
            throw t;
        }
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
    @Order(2)
    public void testRedisSyncException() {
        RedisCommands<String, String> myRedisCommands = getRedisConnection(RedisCommands.class, true);
        assertThrows(RedisCommandTimeoutException.class, () -> myRedisCommands.set(key, value),
                "An exception was thrown at LettuceInterceptor");
    }

    @Test
    @DisplayName("Tests whether Redis sync interceptor connection can read and write")
    @Order(3)
    public void testRedisSync() {
        RedisCommands<String, String> myRedisCommands = getRedisConnection(RedisCommands.class, false);
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }

    @Test
    @DisplayName("Tests whether Redis async interceptor can inject a timeout exception")
    @Order(4)
    public void testRedisAsyncException() {
        RedisAsyncCommands<String, String> myRedisCommands = getRedisConnection(RedisAsyncCommands.class, true);
        assertThrows(RedisCommandTimeoutException.class, () -> myRedisCommands.set(key, value),
                "An exception was thrown at LettuceInterceptor");
    }

    @Test
    @DisplayName("Tests whether Redis async interceptor connection can read and write")
    @Order(5)
    public void testRedisAsync() throws ExecutionException, InterruptedException {
        RedisAsyncCommands<String, String> myRedisCommands = getRedisConnection(RedisAsyncCommands.class, false);
        myRedisCommands.set(key, value).get();
        String retrievedValue = myRedisCommands.get(key).get();
        assertEquals(value, retrievedValue);
    }

    @Test
    @DisplayName("Tests whether Redis reactive interceptor can inject a timeout exception")
    @Order(6)
    public void testRedisReactiveException() {
        RedisReactiveCommands<String, String> myRedisCommands = getRedisConnection(RedisReactiveCommands.class, true);
        assertThrows(RedisCommandTimeoutException.class, () -> myRedisCommands.set(key, value).subscribe(),
                "An exception was thrown at LettuceInterceptor");
    }

    @Test
    @DisplayName("Tests whether Redis reactive interceptor connection can read and write")
    @Order(7)
    public void testRedisReactive() {
        RedisReactiveCommands<String, String> myRedisCommands = getRedisConnection(RedisReactiveCommands.class, false);
        Mono<String> set = myRedisCommands.set(key, value);
        Mono<String> get = myRedisCommands.get(key);
        set.subscribe();
        assertEquals(value, get.block());
    }


}
