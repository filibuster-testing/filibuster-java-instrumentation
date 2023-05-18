package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.RedisSingleGetStringByzantineFaultAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JUnitRedisFilibusterBuggyFallbackByzantineTest {
    static final String key = "test";
    static final String value = "example";

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
        // Prime the cache: Insert key-value-pair into Redis
        RedisClientService.getInstance().redisClient.connect().sync().set(key, value);
    }

    @DisplayName("Tests reading data from Redis")
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleGetStringByzantineFaultAnalysisConfigurationFile.class)
    public void testRedisHello() {
        ManagedChannel apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();

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
