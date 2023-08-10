package cloud.filibuster.functional.java.redis.example.applications;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTransformBitInByteArrAndGRPCExceptionAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetSessionLocationPositiveTest extends JUnitAnnotationBaseTest {
    private final static ArrayList<String> testFaults = new ArrayList<>();
    private static int numberOfTestExecutions = 0;
    private static ManagedChannel apiChannel;
    private static int sessionSize;
    private static APIServiceGrpc.APIServiceBlockingStub apiService;

    @BeforeAll
    public static void beforeAll() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
        RedisClientService.getInstance();

        // Initialize API channel and service
        apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
        apiService = APIServiceGrpc.newBlockingStub(apiChannel);
    }

    @AfterAll
    public static void destruct() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
        stopHelloServerAndWaitUntilUnavailable();
        apiChannel.shutdownNow();
    }

    @DisplayName("Tests whether a session location can be created and then retrieved from Redis - " +
            "Inject transformer faults where random bits are flipped in the byte array.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAndGRPCExceptionAnalysisConfigurationFile.class,
            maxIterations = 1000)
    public void testCreateAndGetSessionLocationFromRedis() throws IOException, InterruptedException {
        numberOfTestExecutions++;

        startAPIServerAndWaitUntilAvailable();
        RedisClientService.getInstance();

        // Initialize API channel and service
        apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
        apiService = APIServiceGrpc.newBlockingStub(apiChannel);

        String uid = "Joe";
        String location = "US";

        try {
            // Create session
            Hello.CreateSessionResponse session = createSession(uid, location);
            assertNotNull(session);
            sessionSize = session.getSessionSize();

            // Retrieve session
            Hello.GetLocationFromSessionResponse retrievedLocation = getLocation(session.getSessionId());
            assertNotNull(retrievedLocation);
        } catch (Throwable t) {
            testFaults.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
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

    private static Hello.GetLocationFromSessionResponse getLocation(String sessionId) {
        Hello.GetSessionRequest sessionRequest = Hello.GetSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
        return apiService.getLocationFromSession(sessionRequest);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // Number of execution is |BFI fault space| * |grpc fault space| + reference execution
    // For the BFI fault space, we have a byte array with 44 bytes. Therefore, the fault space is 36 * 8 = 288
    // For the gRPC fault space, we inject only one fault in the Hello service. Therefore, the fault space is 1 + 1 = 2
    // The +1 comes from the iteration where no gRPC fault is injected
    // In total, we have 288 * 2 + 1 = 577 test executions
    public void testNumExecutions() {
        assertEquals(1 + sessionSize * 2, numberOfTestExecutions);
    }

    @DisplayName("Assert the exception UNAVAILABLE is found when a BFI and a GRPC fault are simultaneously injected")
    @Test
    @Order(3)
    public void testGRPCAndBFIFaultFound() {
        // This fault can only be detected in the iterations where a bit in the key "loc" is mutated
        // and, simultaneously, a gRPC fault is injected.
        // The length of the key "loc" is 3. Therefore, there are 3 * 8 = 24 iterations where the fault will be found
        // That fault was found in 24 / 577 = 4.2% of the executions
        int helloFaults = testFaults.stream().filter(e -> e.contains("UNAVAILABLE")).collect(Collectors.toList()).size();
        assertEquals(24, helloFaults);
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
        // Out of 577 executions, 308 were deserialization faults (308 / 577 = 53%)
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // 24 iterations where a gRPC fault and BFI fault at key "loc" are simultaneously injected
        int combinedGrpcAndBFIFaults = testFaults.stream().filter(e -> e.contains("UNAVAILABLE")).collect(Collectors.toList()).size();

        // Total faults should be 308 + 24 = 332 faults
        // This shows that only 332 / 577 = 57% of the executions actually invoked exceptions
        // The rest of the executions were successful, although a bit was flipped
        assertEquals(332, deserializationFaults + combinedGrpcAndBFIFaults);

        // All faults should be either deserialization faults or from the hello service
        assertEquals(testFaults.size(), deserializationFaults + combinedGrpcAndBFIFaults);
    }

}
