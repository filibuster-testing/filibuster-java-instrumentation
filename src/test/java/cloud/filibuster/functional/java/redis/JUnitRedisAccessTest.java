package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisConnection;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.*;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@FilibusterConditionalByEnvironmentSuite
public class JUnitRedisAccessTest extends JUnitAnnotationBaseTest {

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
    public void testRedisWriteAndRead() {
        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();
        String key = "test";
        String value = "example";

        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
        Hello.RedisWriteRequest writeRequest = Hello.RedisWriteRequest.newBuilder().setKey(key).setValue(value).build();
        Hello.RedisReply reply = blockingStub.redisWrite(writeRequest);
        assertEquals("1", reply.getValue());  // "1" indicates that the key-value-pair was inserted successfully

        Hello.RedisReadRequest readRequest = Hello.RedisReadRequest.newBuilder().setKey(key).build();
        reply = blockingStub.redisRead(readRequest);
        assertEquals(value, reply.getValue());
    }
}
