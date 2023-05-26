package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.byzantine.redis.RedisSingleGetBasicStringByzantineFaultAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @DisplayName("Tests reading data from APIService with Redis byzantine fault injection")
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleGetBasicStringByzantineFaultAnalysisConfigurationFile.class)
    public void testRedisByzantineBuggyFallback() {
        ManagedChannel apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.RedisRequest readRequest = Hello.RedisRequest.newBuilder().setKey(key).build();
            Hello.RedisReply reply = blockingStub.redisHello(readRequest);
            assertEquals(value, reply.getValue());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                assertTrue(wasFaultInjectedOnService(REDIS_MODULE_NAME), "Fault was not injected on the Redis module");
                assertTrue(wasFaultInjectedOnMethod(REDIS_MODULE_NAME, "get"), "Fault was not injected on the expected Redis method");
                String expectedErrorMessage = "INTERNAL: INTERNAL: java.lang.Exception: An exception was thrown at Hello service\n" +
                        "MyAPIService could not process the request as an exception was thrown";
                assertTrue(t.getMessage().contains(expectedErrorMessage), "Unexpected return error message for injected byzantine fault");
                return;
            }
            throw t;
        }
    }
}
