package cloud.filibuster.functional.java.redis.scenarios;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.GrpcAndRedisStringExceptionAndTransformerAndByzantineAnalysisConfigurationFile;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitFaultyRedisFaultyGRPCWithDataNondeterminismTest extends JUnitAnnotationBaseTest {
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private static final Logger logger = Logger.getLogger(JUnitFaultyRedisFaultyGRPCWithDataNondeterminismTest.class.getName());
    private static final ArrayList<String> keys = new ArrayList<>();
    private static final ArrayList<String> values = new ArrayList<>();

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;

        // Generate random keys and values and add them to Redis
        RedisCommands<String, String> redisCommands = statefulRedisConnection.sync();
        for (int i = 0; i < 3; i++) {
            String key = getRandomString();
            String value = getRandomString();
            keys.add(key);
            values.add(value);
            redisCommands.set(key, value);
        }

        startHelloServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
    }

    @DisplayName("Tests whether Redis sync interceptor connection can read and write")
    @Order(1)
    @TestWithFilibuster(
            analysisConfigurationFile = GrpcAndRedisStringExceptionAndTransformerAndByzantineAnalysisConfigurationFile.class,
            maxIterations = 300,
            dataNondeterminism = true
    )
    public void testRedisSync() {
        // Send GRPC request with random name
        sayHelloAndAssert(getRandomString());

        // Prepare Redis interceptor
        StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
        RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

        // Get key from Redis and assert correct value
        for (int i = 0; i < 2; i++) {
            getFromRedisAndAssert(myRedisCommands, keys.get(i), values.get(i));
        }

        // Send GRPC request with random name
       sayHelloAndAssert(getRandomString());
    }


    private static void sayHelloAndAssert(String name) {
        try {
            ManagedChannel helloChannel = ManagedChannelBuilder.forAddress(Networking.getHost("hello"), Networking.getPort("hello")).usePlaintext().build();

            ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("hello");
            Channel channel = ClientInterceptors.intercept(helloChannel, clientInterceptor);

            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(name).build();
            Hello.HelloReply helloReply = blockingStub.hello(request);

            helloChannel.shutdown();

            assertEquals(String.format("Hello, %s!!", name), helloReply.getMessage());
        } catch (Throwable e) {
            logger.log(Level.INFO, "getFromRedis threw an exception: " + e);
        }
    }

    private static void getFromRedisAndAssert(RedisCommands<String, String> redisCommand, String key, String value) {
        try {
            String result = redisCommand.get(key);
            assertEquals(value, result);
        } catch (Throwable e) {
            logger.log(Level.INFO, "getFromRedis threw an exception: " + e);
        }
    }

    private static String getRandomString() {
        return RandomStringUtils.randomAlphanumeric(2);
    }
}
