package cloud.filibuster.functional.java.redis.example.applications;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTransformBitInByteArrAnalysisConfigurationFile;
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
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterGetSessionNegativeTest extends JUnitAnnotationBaseTest {
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

        apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
    }

    @AfterAll
    public static void destruct() {
        apiChannel.shutdown();
    }

    @DisplayName("Tests whether a session can be retrieved from Redis - Inject transformer faults where random bits are flipped in the byte array.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAnalysisConfigurationFile.class,
            maxIterations = 1000)
    public void testGetSessionFromRedis() {
        numberOfTestExecutions++;

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.GetSessionRequest sessionRequest = Hello.GetSessionRequest.newBuilder().setSessionId(sessionId).build();
            Hello.GetSessionResponse reply = blockingStub.getSession(sessionRequest);
            assertNotNull(reply.getSession());
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
    // Number of execution is |BFI fault space| + reference execution
    // For the BFI fault space, we have a byte array with 44 bytes. Therefore, the fault space is 44 * 8 = 352
    // In total, we have 352 + 1 = 353 test executions
    public void testNumExecutions() {
        assertEquals(1 + sessionBytes.length * 8, numberOfTestExecutions);
    }

    @DisplayName("Assert all faults that occurred were deserialization faults")
    @Test
    @Order(3)
    public void testNumDeserializationFaults() {
        // In this scenario, the bit transformations should only cause deserialization faults
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // Out of 705 executions, 154 were deserialization faults
        // This shows that only 154 / 705 = 21.8% of the executions actually caused faults
        // The rest of the executions were successful, although a bit was flipped
        assertEquals(154, deserializationFaults);

        // All faults should be deserialization faults
        assertEquals(testFaults.size(), deserializationFaults);
    }

    @DisplayName("Assert the second level cache fault was not found")
    @Test
    @Order(4)
    public void testSecondLevelCacheFaultNotFound() {
        // The bit transformations should only cause deserialization faults
        // In all the 705 executions, none of the injected faults leads us to discover the
        // second level cache down fault
        int cacheDownFault = testFaults.stream().filter(e -> e.contains("Redis second level cache is down.")).collect(Collectors.toList()).size();
        assertEquals(0, cacheDownFault);
    }

}
