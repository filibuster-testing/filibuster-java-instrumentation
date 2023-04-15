package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        // Insert key-value-pair into Redis
        String key = "test";
        String value = "example";
        redisConnection.sync().set(key, value);

        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
        Hello.RedisReadRequest readRequest = Hello.RedisReadRequest.newBuilder().setKey(key).build();
        Hello.RedisReply reply = blockingStub.redisHello(readRequest);
        assertEquals(value, reply.getValue());  // This test would fail if a Redis fault is injected
    }
}
