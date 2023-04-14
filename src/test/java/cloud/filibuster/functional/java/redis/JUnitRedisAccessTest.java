package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FilibusterConditionalByEnvironmentSuite
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {

    @BeforeAll
    public static void setUp() {
        RedisConnection.getInstance();
    }

    @Test
    public void testRedisWriteAndRead() {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();
        String key = "test";
        String value = "example";

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.RedisWriteRequest writeRequest = Hello.RedisWriteRequest.newBuilder().setKey(key).setValue(value).build();
        Hello.RedisReply reply = blockingStub.redisWrite(writeRequest);
        assertEquals("1", reply.getValue());

        Hello.RedisReadRequest readRequest = Hello.RedisReadRequest.newBuilder().setKey(key).build();
        reply = blockingStub.redisRead(readRequest);
        assertEquals(value, reply.getValue());
    }
}
