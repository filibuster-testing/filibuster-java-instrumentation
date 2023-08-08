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
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetSessionNegativeTest extends JUnitAnnotationBaseTest {
    private final static ArrayList<String> testFaults = new ArrayList<>();
    private static int numberOfTestExecutions = 0;
    private static ManagedChannel apiChannel;
    private static int sessionSize;
    private static APIServiceGrpc.APIServiceBlockingStub apiService;

    @AfterAll
    public static void destruct() {
        apiChannel.shutdown();
    }

    @DisplayName("Tests whether a session can be retrieved from Redis - " +
            "Inject transformer faults where random bits are flipped in the byte array.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAnalysisConfigurationFile.class,
            maxIterations = 1000)
    public void testCreateAndGetSessionFromRedis() throws IOException, InterruptedException {
        numberOfTestExecutions++;

        startAPIServerAndWaitUntilAvailable();
        RedisClientService.getInstance();

        // Initialize API channel and service
        apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
        apiService = APIServiceGrpc.newBlockingStub(apiChannel);

        String uid = "JohnS";
        String location = "US";

        try {
            // Create session
            Hello.CreateSessionResponse session = createSession(uid, location);
            assertNotNull(session);
            sessionSize = session.getSessionSize();

            // Retrieve session
            Hello.GetSessionResponse retrievedSession = getSession(session.getSessionId());
            JSONObject retrievedSessionJO = new JSONObject(retrievedSession.getSession());
            assertNotNull(retrievedSessionJO);

        } catch (Throwable t) {
            testFaults.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("io.lettuce.core.api.sync.RedisStringCommands"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/get"), "Fault was not injected on the expected Redis method: " + t);

        }
    }

    private static Hello.CreateSessionResponse createSession(String userId, String location) {
        Hello.CreateSessionRequest sessionRequest = Hello.CreateSessionRequest.newBuilder()
                .setUserId(userId)
                .setLocation(location)
                .build();
        return apiService.createSession(sessionRequest);
    }

    private static Hello.GetSessionResponse getSession(String sessionId) {
        Hello.GetSessionRequest sessionRequest = Hello.GetSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        return apiService.getSession(sessionRequest);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // Number of execution is reference execution + |BFI fault space|
    // For the BFI fault space, we can inject a fault at every bit in the session
    // In total, we have 1 + 353 = 354 test executions
    public void testNumExecutions() {
        assertEquals(1 + sessionSize, numberOfTestExecutions);
    }

    @DisplayName("Assert all faults that occurred were deserialization faults")
    @Test
    @Order(3)
    public void testNumDeserializationFaults() {
        // In this scenario, the bit transformations should only cause deserialization faults
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // Out of 354 executions, 154 were deserialization faults
        // This shows that only 154 / 354 = 43.5% of the executions actually caused faults
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
