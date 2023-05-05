package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.lettuce.FilibusterRedisClientInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterTest extends JUnitAnnotationBaseTest {
    String key = "test";
    String value = "example";

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
    }

    @Test
    @DisplayName("Tests whether Redis sync interceptor can inject a timeout exception")
    @Order(1)
    @TestWithFilibuster(maxIterations = 1)
    public void testRedisSyncException() {
        RedisCommands<String, String> myRedisCommands = new FilibusterRedisClientInterceptor()
                .getConnection(RedisCommands.class);
        assertThrows(RedisCommandTimeoutException.class, () -> myRedisCommands.set(key, value),
                "An exception was thrown at LettuceInterceptor");
    }

    @Test
    @DisplayName("Tests whether Redis sync interceptor connection can read and write")
    @Order(2)
    @TestWithFilibuster(maxIterations = 2)
    public void testRedisSync() {
        RedisCommands<String, String> myRedisCommands = new FilibusterRedisClientInterceptor()
                .getConnection(RedisCommands.class);
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }

    @Test
    @DisplayName("Tests reading data from Redis")
    @Order(3)
    @TestWithFilibuster(maxIterations = 2)
    public void testRedisHello() {
        ManagedChannel apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
        // Prime the cache: Insert key-value-pair into Redis
        RedisConnection.getInstance().connection.sync().set(key, value);

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

}
