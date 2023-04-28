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
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {
    private static StatefulRedisConnection<String, String> redisConnection;

    @BeforeAll
    public static void setUp() {
        redisConnection = RedisConnection.getInstance().connection;
    }

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
        String key = "test";
        String value = "example";
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

    @Test
    @DisplayName("Tests whether Redis interceptor can inject a timeout exception")
    @Order(2)
    public void testRedisInterceptedTimeoutExceptionThrowing() {
        RedisCommands<Object, Object> myRedisCommands = LettuceInterceptedConnection.create(redisConnection);
        LettuceInterceptor.isFaultInjected = true;
        String key = "test";
        String value = "example";
        assertThrows(RedisCommandTimeoutException.class, () -> myRedisCommands.set(key, value),
                "An exception was thrown at LettuceInterceptor");
    }

    @Test
    @DisplayName("Tests whether Redis interceptor connection can read and write")
    @Order(3)
    public void testRedisInterceptedReturningData() {
        RedisCommands<Object, Object> myRedisCommands = LettuceInterceptedConnection.create(redisConnection);
        LettuceInterceptor.isFaultInjected = false;
        String key = "test";
        String value = "example";
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }
}
