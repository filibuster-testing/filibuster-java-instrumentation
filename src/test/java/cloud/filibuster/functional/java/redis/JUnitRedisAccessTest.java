package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {
    String key = "test";
    String value = "example";

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
    }

    @Test
    @DisplayName("Tests reading data from Redis")
    @Order(1)
    public void testRedisHello() {
        ManagedChannel apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
        // Prime the cache: Insert key-value-pair into Redis
        RedisClientService.getInstance().redisClient.connect().sync().set(key, value);

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

    @Test
    @DisplayName("Tests whether Redis sync interceptor connection can read and write")
    @Order(3)
    public void testRedisSync() {
        StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>().getProxy(StatefulRedisConnection.class);
        RedisCommands<String, String> myRedisCommands =  myStatefulRedisConnection.sync();
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }

    @Test
    @DisplayName("Tests whether Redis async interceptor connection can read and write")
    @Order(5)
    public void testRedisAsync() throws ExecutionException, InterruptedException {
        StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>().getProxy(StatefulRedisConnection.class);
        RedisAsyncCommands<String, String> myRedisCommands  =  myStatefulRedisConnection.async();
        myRedisCommands.set(key, value).get();
        String retrievedValue = myRedisCommands.get(key).get();
        assertEquals(value, retrievedValue);
    }

    @Test
    @DisplayName("Tests whether Redis reactive interceptor connection can read and write")
    @Order(7)
    public void testRedisReactive() {
        StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>().getProxy(StatefulRedisConnection.class);
        RedisReactiveCommands<String, String> myRedisCommands  =  myStatefulRedisConnection.reactive();
        Mono<String> set = myRedisCommands.set(key, value);
        Mono<String> get = myRedisCommands.get(key);
        set.subscribe();
        assertEquals(value, get.block());
    }

}
