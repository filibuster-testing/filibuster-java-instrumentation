package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.redis.RedisSingleFaultCommandInterruptedExceptionAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.shaded.com.google.common.base.Stopwatch;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.com.google.common.base.Stopwatch.createStarted;

public class JUnitRedisFilibusterRetryTest {
    private static final String key = "username";
    private static final String value = "Filibuster";

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
        // Prime the cache: Insert key-value-pair into Redis
        RedisClientService.getInstance().redisClient.connect().sync().set(key, value);
    }

    @DisplayName("Tests reading data from APIService with Redis byzantine fault injection")
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleFaultCommandInterruptedExceptionAnalysisConfigurationFile.class)
    public void testRedisRetry() {
        Stopwatch stopwatch = createStarted();  // Start stopwatch

        ManagedChannel apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.RedisRequest readRequest = Hello.RedisRequest.newBuilder().setKey(key).build();
            Hello.RedisReply reply = blockingStub.redisHelloRetry(readRequest);
            assertEquals("Hello, " + value + "!!", reply.getValue());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                assertTrue(wasFaultInjectedOnService(REDIS_MODULE_NAME), "Fault was not injected on the Redis module");
                assertTrue(wasFaultInjectedOnMethod(REDIS_MODULE_NAME, "io.lettuce.core.RedisFuture.await"), "Fault was not injected on the expected Redis method");
                String expectedErrorMessage = "Command interrupted";
                assertTrue(t.getMessage().contains(expectedErrorMessage), "Unexpected return error message for injected byzantine fault");
            } else {
                throw t;
            }
        }

        long elapsedTime = stopwatch.elapsed(TimeUnit.SECONDS);
        assertTrue(elapsedTime < 3, "Test took more than 1 seconds to complete. It took: " + elapsedTime);
    }
}
