package cloud.filibuster.functional.java.redis.scenarios;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.FilibusterSearchStrategy;
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
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FaultyRedisFaultyGRPCWithSuppressCombinationsBFSTest extends JUnitAnnotationBaseTest {
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private static final Logger logger = Logger.getLogger(FaultyRedisFaultyGRPCWithSuppressCombinationsBFSTest.class.getName());
    private static final ArrayList<String> keys = new ArrayList<>();
    private static final ArrayList<String> values = new ArrayList<>();
    private static int numberOfBFSExecutions = 0;
    private static int numberOfDFSExecutions = 0;
    private static final ArrayList<String> BFSExceptions = new ArrayList<>();
    private static final ArrayList<String> DFSExceptions = new ArrayList<>();
    private static String name;
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

        name = getRandomString();

        startHelloServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
    }

    @DisplayName("Tests the scenario where faults are injected in both Redis and the GRPC client. GRPC calls are issued before and after the Redis call. " +
            "SuppressCombinations is set to true, search strategy is BFS")
    @Order(1)
    @TestWithFilibuster(
            analysisConfigurationFile = GrpcAndRedisStringExceptionAndTransformerAndByzantineAnalysisConfigurationFile.class,
            maxIterations = 30,
            suppressCombinations = true,
            searchStrategy = FilibusterSearchStrategy.BFS
    )
    public void testRedisSyncBFS() {
        numberOfBFSExecutions++;

        // Send GRPC request
        sayHelloAndAssert(name, FilibusterSearchStrategy.BFS);

        // Prepare Redis interceptor
        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
        RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

        // Get key from Redis and assert correct value
        for (int i = 0; i < 3; i++) {
            getFromRedisAndAssert(myRedisCommands, keys.get(i), values.get(i), FilibusterSearchStrategy.BFS);
        }

        // Send GRPC request
        sayHelloAndAssert(name, FilibusterSearchStrategy.BFS);
    }

    @DisplayName("Tests the scenario where faults are injected in both Redis and the GRPC client. GRPC calls are issued before and after the Redis call. " +
            "SuppressCombinations is set to true, search strategy is DFS")
    @Order(2)
    @TestWithFilibuster(
            analysisConfigurationFile = GrpcAndRedisStringExceptionAndTransformerAndByzantineAnalysisConfigurationFile.class,
            maxIterations = 30,
            suppressCombinations = true,
            searchStrategy = FilibusterSearchStrategy.DFS
    )
    public void testRedisSyncDFS() {
        numberOfDFSExecutions++;

        // Send GRPC request
        sayHelloAndAssert(name, FilibusterSearchStrategy.DFS);

        // Prepare Redis interceptor
        StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
        RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

        // Get key from Redis and assert correct value
        for (int i = 0; i < 3; i++) {
            getFromRedisAndAssert(myRedisCommands, keys.get(i), values.get(i), FilibusterSearchStrategy.DFS);
        }

        // Send GRPC request
        sayHelloAndAssert(name, FilibusterSearchStrategy.DFS);
    }

    @DisplayName("Test whether BFS and DFS generate the same number of exceptions")
    @Order(3)
    @Test
    public void testBFSAndDFSExceptions() {
        assertEquals(BFSExceptions.size(), DFSExceptions.size());
    }

    @DisplayName("Test whether total number of executions is equal in BFS and DFS")
    @Order(4)
    @Test
    public void testNumExecution() {
        assertEquals(numberOfBFSExecutions, numberOfDFSExecutions);
    }

    @DisplayName("Assert correct BFS fault messages")
    @Order(5)
    @Test
    public void testBFSFaultMessages() {
        List<String> transformerFaults = getMatchesInFaultMessages("expected: <..> but was: <..>", BFSExceptions);
        List<String> nullFaults = getMatchesInFaultMessages("expected: <..> but was: <null>", BFSExceptions);
        List<String> timeoutException = getMatchesInFaultMessages("Command timed out after 100 millisecond\\(s\\)", BFSExceptions);
        List<String> grpcException = getMatchesInFaultMessages("UNAVAILABLE", BFSExceptions);

        assertEquals(6, transformerFaults.size());
        assertEquals(3, nullFaults.size());
        assertEquals(3, timeoutException.size());
        assertEquals(2, grpcException.size());
    }

    @DisplayName("Assert correct DFS fault messages")
    @Order(6)
    @Test
    public void testDFSFaultMessages() {
        List<String> transformerFaults = getMatchesInFaultMessages("expected: <..> but was: <..>", DFSExceptions);
        List<String> nullFaults = getMatchesInFaultMessages("expected: <..> but was: <null>", DFSExceptions);
        List<String> timeoutException = getMatchesInFaultMessages("Command timed out after 100 millisecond\\(s\\)", DFSExceptions);
        List<String> grpcException = getMatchesInFaultMessages("UNAVAILABLE", DFSExceptions);

        assertEquals(6, transformerFaults.size());
        assertEquals(3, nullFaults.size());
        assertEquals(3, timeoutException.size());
        assertEquals(2, grpcException.size());
    }

    private static List<String> getMatchesInFaultMessages(String regex, List<String> messageList) {
        Pattern pattern = Pattern.compile(regex);

        return messageList
                .stream()
                .filter(e -> pattern.matcher(e).matches())
                .collect(Collectors.toList());
    }

    private static void sayHelloAndAssert(String name, FilibusterSearchStrategy searchStrategy) {
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

            if (searchStrategy == FilibusterSearchStrategy.BFS) {
                BFSExceptions.add(e.getMessage());
            } else {
                DFSExceptions.add(e.getMessage());
            }
        }
    }

    private static void getFromRedisAndAssert(RedisCommands<String, String> redisCommand, String key, String value, FilibusterSearchStrategy searchStrategy) {
        try {
            String result = redisCommand.get(key);
            assertEquals(value, result);
        } catch (Throwable e) {
            logger.log(Level.INFO, "getFromRedis threw an exception: " + e);

            if (searchStrategy == FilibusterSearchStrategy.BFS) {
                BFSExceptions.add(e.getMessage());
            } else {
                DFSExceptions.add(e.getMessage());
            }
        }
    }

    private static String getRandomString() {
        return String.valueOf(rand.nextInt(90) + 10);
    }
}
