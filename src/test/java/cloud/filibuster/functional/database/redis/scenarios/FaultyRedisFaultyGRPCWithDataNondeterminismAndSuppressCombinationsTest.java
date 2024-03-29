package cloud.filibuster.functional.database.redis.scenarios;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static cloud.filibuster.junit.assertions.protocols.GrpcAssertions.wasFaultInjectedOnMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FaultyRedisFaultyGRPCWithDataNondeterminismAndSuppressCombinationsTest extends JUnitAnnotationBaseTest {
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private static final Logger logger = Logger.getLogger(FaultyRedisFaultyGRPCWithDataNondeterminismAndSuppressCombinationsTest.class.getName());
    private static final ArrayList<String> keys = new ArrayList<>();
    private static final ArrayList<String> values = new ArrayList<>();
    private static int numberOfExecution = 0;
    private static final HashSet<String> actualFaultMessages = new HashSet<>();
    private static final Random rand = new Random(0);

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

    @DisplayName("Tests the scenario where faults are injected in both Redis and the GRPC client. GRPC calls are issued before and after the Redis call. " +
            "Both dataNondeterminism and suppressCombinations are true.")
    @Order(1)
    @TestWithFilibuster(
            analysisConfigurationFile = GrpcAndRedisStringExceptionAndTransformerAndByzantineAnalysisConfigurationFile.class,
            maxIterations = 30,
            dataNondeterminism = true,
            suppressCombinations = true
    )
    public void testRedisSync() {
        numberOfExecution++;

        // Send GRPC request with random name
        sayHelloAndAssert(getRandomString());

        // Prepare Redis interceptor
        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
        RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

        // Get key from Redis and assert correct value
        for (int i = 0; i < 3; i++) {
            getFromRedisAndAssert(myRedisCommands, keys.get(i), values.get(i));
        }

        // Send GRPC request with random name
        sayHelloAndAssert(getRandomString());
    }

    @DisplayName("Assert correct number of test executions")
    @Order(2)
    @Test
    public void testNumberOfExecutions() {
        // We inject 4 faults per Redis get call: 2 transformer faults (one for each char),
        // 1 byzantine execution (injecting null) and 1 exception execution (injecting RedisCommandTimeoutException)
        // Per GRPC call, we inject one UNAVAILABLE exception.
        // Since suppressCombinations is true, Redis get is called 3 times and GRPC calls are issued twice,
        // this leads to 1 reference execution + 4*3 + 2 = 15 executions
        assertEquals(15, numberOfExecution);
    }

    @DisplayName("Assert number of faults")
    @Order(3)
    @Test
    public void testNumberOfFaults() {
        // For each of the 2 Redis get call, we inject 4 faults: 2 transformer faults (one for each char), 1 byzantine
        // fault and 1 exception. The error message of the exception is the same for all Redis get calls.
        // Therefore, we expect 1 + 3 * 3 = 10 Redis fault messages
        // Additionally, we have 2 GRPC calls. Each can throw an UNAVAILABLE exception.
        // The exception message is the same for both GRPC calls.
        // Therefore, we expect 10 + 1 = 11 fault messages
        assertEquals(11, actualFaultMessages.size());
    }

    @DisplayName("Assert correct fault messages")
    @Order(4)
    @Test
    public void testFaultMessages() {
        List<String> transformerFaults = getMatchesInFaultMessages("expected: <..> but was: <..>");
        List<String> nullFaults = getMatchesInFaultMessages("expected: <..> but was: <null>");
        List<String> timeoutException = getMatchesInFaultMessages("Command timed out after 100 millisecond\\(s\\)");
        List<String> grpcException = getMatchesInFaultMessages("UNAVAILABLE");

        assertEquals(6, transformerFaults.size());
        assertEquals(3, nullFaults.size());
        assertEquals(1, timeoutException.size());
        assertEquals(1, grpcException.size());
    }

    private static void sayHelloAndAssert(String name) {
        try {
            ManagedChannel helloChannel = ManagedChannelBuilder.forAddress(Networking.getHost("hello"), Networking.getPort("hello")).usePlaintext().build();

            ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("hello");
            Channel channel = ClientInterceptors.intercept(helloChannel, clientInterceptor);

            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName(name).build();
            Hello.HelloReply helloReply = blockingStub.hello(request);

            helloChannel.shutdownNow();

            assertEquals(String.format("Hello, %s!!", name), helloReply.getMessage());
        } catch (Throwable e) {
            logger.log(Level.INFO, "getFromRedis threw an exception: " + e);
            actualFaultMessages.add(e.getMessage());
            assertTrue(wasFaultInjected());
            assertTrue(wasFaultInjectedOnMethod(HelloServiceGrpc.getHelloMethod()));
        }
    }

    private static void getFromRedisAndAssert(RedisCommands<String, String> redisCommand, String key, String value) {
        try {
            String result = redisCommand.get(key);
            assertEquals(value, result);
        } catch (Throwable e) {
            logger.log(Level.INFO, "getFromRedis threw an exception: " + e);
            actualFaultMessages.add(e.getMessage());
            assertTrue(wasFaultInjected());
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/get"));
        }
    }

    private static List<String> getMatchesInFaultMessages(String regex) {
        Pattern pattern = Pattern.compile(regex);

        return actualFaultMessages
                .stream()
                .filter(e -> pattern.matcher(e).matches())
                .collect(Collectors.toList());
    }

    private static String getRandomString() {
        return String.valueOf(rand.nextInt(90) + 10);
    }}
