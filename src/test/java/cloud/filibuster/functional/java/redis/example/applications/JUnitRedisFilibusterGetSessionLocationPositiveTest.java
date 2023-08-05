package cloud.filibuster.functional.java.redis.example.applications;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTransformBitInByteArrAndGRPCExceptionAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterGetSessionLocationPositiveTest extends JUnitAnnotationBaseTest {
    static JSONObject referenceSession;
    static byte[] sessionBytes;
    static String sessionId = "abc123";
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private final static ArrayList<String> testFaults = new ArrayList<>();
    private static int numberOfTestExecutions = 0;
    private static ManagedChannel apiChannel;

    @BeforeAll
    public static void primeCache() throws IOException, InterruptedException {
        referenceSession = new JSONObject();
        referenceSession.put("uid", "JohnS.");
        referenceSession.put("location", "US");
        referenceSession.put("iat", "123");

        sessionBytes = referenceSession.toString().getBytes(Charset.defaultCharset());

        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(sessionId, sessionBytes);

        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();

        apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
    }

    @AfterAll
    public static void destruct() {
        apiChannel.shutdown();
    }

    @DisplayName("Tests whether a session location can be retrieved from Redis - Inject transformer faults where random bits are flipped in the byte array.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAndGRPCExceptionAnalysisConfigurationFile.class,
            maxIterations = 1000)
    public void testGetSessionLocationFromRedis() {
        numberOfTestExecutions++;

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.GetSessionRequest locationRequest = Hello.GetSessionRequest.newBuilder().setSessionId(sessionId).build();
            Hello.GetLocationFromSessionResponse reply = blockingStub.getLocationFromSession(locationRequest);
            assertNotNull(reply.getLocation());
        } catch (Throwable t) {
            testFaults.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("io.lettuce.core.api.sync.RedisStringCommands"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnMethod("io.lettuce.core.api.sync.RedisStringCommands/get"), "Fault was not injected on the expected Redis method: " + t);

        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // Number of execution is |BFI fault space| * |grpc fault space| + reference execution
    // For the BFI fault space, we have a byte array with 44 bytes. Therefore, the fault space is 44 * 8 = 352
    // For the gRPC fault space, we inject only one fault in the Hello service. Therefore, the fault space is 1 + 1 = 2
    // The +1 comes from the iteration where no gRPC fault is injected
    // In total, we have 352 * 2 + 1 = 705 test executions
    public void testNumExecutions() {
        assertEquals(1 + (sessionBytes.length * 8) * 2, numberOfTestExecutions);
    }

    @DisplayName("Assert the exception UNAVAILABLE is found when a BFI and a GRPC fault are simultaneously injected")
    @Test
    @Order(3)
    public void testGRPCAndBFIFaultFound() {
        // This fault can only be detected in the iterations where a bit in the key "location" is mutated
        // and, simultaneously, a gRPC fault is injected.
        // The length of the key "location" is 8. Therefore, there are 8 * 8 = 64 iterations where the fault will be found
        // That fault was found in 64 / 705 = 9% of the executions
        int helloFaults = testFaults.stream().filter(e -> e.contains("UNAVAILABLE")).collect(Collectors.toList()).size();
        assertEquals(64, helloFaults);
    }

    @DisplayName("Assert the exception 'session not found' was not found")
    @Test
    @Order(4)
    public void testSessionNotFound() {
        // Assert that the "session not found" exception was not found.
        int sessionNotFound = testFaults.stream().filter(e -> e.contains("Retrieved value is null. Session was not found")).collect(Collectors.toList()).size();
        assertEquals(0, sessionNotFound);
    }

    @DisplayName("Assert all faults were deserialization faults or from the hello service")
    @Test
    @Order(5)
    public void testNumDeserializationAndHelloFaults() {
        // Out of 705 executions, 308 were deserialization faults (308 / 705 = 44%)
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // 64 iterations where a gRPC fault and BFI fault at key "location" are simultaneously injected
        int combinedGrpcAndBFIFaults = testFaults.stream().filter(e -> e.contains("UNAVAILABLE")).collect(Collectors.toList()).size();

        // Total faults should be 308 + 64 = 372 faults
        // This shows that only 248 / 705 = 35% of the executions were successful
        // The rest of the executions were successful, although a bit was flipped
        assertEquals(372, deserializationFaults + combinedGrpcAndBFIFaults);

        // All faults should be either deserialization faults or from the hello service
        assertEquals(testFaults.size(), deserializationFaults + combinedGrpcAndBFIFaults);
    }

}
