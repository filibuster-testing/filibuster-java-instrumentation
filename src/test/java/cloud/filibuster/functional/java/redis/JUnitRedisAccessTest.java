package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {

    private final String key = "test";
    private final String value = "example";

    @BeforeAll
    public static void setUp() {
        RedisConnection.getInstance();
    }

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
    }

    @Test
    @DisplayName("Tests reading and writing to Redis")
    @Order(1)
    public void testRedisWriteAndRead() {
        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();

        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
        Hello.RedisWriteRequest writeRequest = Hello.RedisWriteRequest.newBuilder().setKey(key).setValue(value).build();
        Hello.RedisReply reply = blockingStub.redisWrite(writeRequest);
        assertEquals("1", reply.getValue());  // "1" indicates that the key-value-pair was inserted successfully

        Hello.RedisReadRequest readRequest = Hello.RedisReadRequest.newBuilder().setKey(key).build();
        reply = blockingStub.redisRead(readRequest);
        assertEquals(value, reply.getValue());
    }

    @Disabled("Disabled until Redis instrumentation is developed")
    @Test
    @DisplayName("Tests whether Redis cache misses are simulated")
    @Order(2)
    public void testRedisCacheMiss() {
        // todo enable test once Redis cache miss instrumentation is created
        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();

        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
        Hello.RedisReadRequest readRequest = Hello.RedisReadRequest.newBuilder().setKey(key).build();  // key is in the Redis store
        Hello.RedisReply reply = blockingStub.redisHello(readRequest);
        assertEquals("io.grpc.StatusRuntimeException: INTERNAL: java.lang.Exception: An exception was thrown",
                reply.getValue());
    }
}
