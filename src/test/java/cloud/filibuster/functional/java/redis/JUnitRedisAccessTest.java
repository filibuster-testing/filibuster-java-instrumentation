package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.lettuce.LettuceInterceptedConnection;
import cloud.filibuster.instrumentation.libraries.lettuce.LettuceInterceptor;
import cloud.filibuster.instrumentation.libraries.lettuce.MyRedisCommands;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.*;
import static cloud.filibuster.junit.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("Tests whether Redis interceptor can inject an exception")
    @Order(2)
    public void testRedisInterceptedExceptionThrowing() {
        MyRedisCommands myRedisCommands = LettuceInterceptedConnection.create(redisConnection);
        LettuceInterceptor.isFaultInjected = true;
        String key = "test";
        String value = "example";
        try {
            myRedisCommands.set(key, value);
            fail("An exception should have been thrown at Redis service");
        } catch (Throwable t) {
            if (LettuceInterceptor.isFaultInjected && t.getCause().getMessage().equals("An exception was thrown at LettuceInterceptor")) {
                return;
            }
            throw t;
        }
    }

    @Test
    @DisplayName("Tests whether Redis interceptor connection can read and write")
    @Order(3)
    public void testRedisInterceptedReturningData() {
        MyRedisCommands myRedisCommands = LettuceInterceptedConnection.create(redisConnection);
        LettuceInterceptor.isFaultInjected = false;
        String key = "test";
        String value = "example";
        myRedisCommands.set(key, value);
        assertEquals(value, myRedisCommands.get(key));
    }
}
