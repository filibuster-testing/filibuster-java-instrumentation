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
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterGetBookTitlePositiveTest extends JUnitAnnotationBaseTest {
    static JSONObject referenceBook;
    static byte[] bookBytes;
    static String bookId = "abc123";
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private final static ArrayList<String> testFaults = new ArrayList<>();
    private static ManagedChannel apiChannel;
    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void primeCache() throws IOException, InterruptedException {
        referenceBook = new JSONObject();
        referenceBook.put("title", "dist sys");
        referenceBook.put("isbn", "12-34");
        referenceBook.put("author", "j. smith");
        referenceBook.put("pages", "230");

        bookBytes = referenceBook.toString().getBytes(Charset.defaultCharset());

        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(bookId, bookBytes);

        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();

        apiChannel = ManagedChannelBuilder.forAddress(Networking.getHost("api_server"), Networking.getPort("api_server")).usePlaintext().build();
    }

    @AfterAll
    public static void destruct() {
        apiChannel.shutdown();
    }

    @DisplayName("Tests whether a book title can be retrieved from Redis - Inject transformer faults where random bits are flipped in the byte array.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAnalysisConfigurationFile.class,
            maxIterations = 1000)
    public void testGetBookTitleFromRedis() {
        numberOfTestExecutions++;

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.GetBookRequest bookRequest = Hello.GetBookRequest.newBuilder().setBookId(bookId).build();
            Hello.GetBookTitleResponse reply = blockingStub.getBookTitle(bookRequest);
            assertNotNull(reply.getTitle());
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
    // 1 for the reference execution and +1 for each bit in the byte array
    // => 1 + bookBytes.length * 8 = 1 + 69 * 8 = 553
    public void testNumExecutions() {
        assertEquals(1 + bookBytes.length * 8, numberOfTestExecutions);
    }

    @DisplayName("Assert the fault at the hello service was found")
    @Test
    @Order(3)
    public void testHelloFaultFound() {
        // The fault in the hello service will be found in every iteration where a bit in the key "title" was mutated
        // The length of the key "title" is 5. Therefore, there are 5 * 8 = 40 iterations where the fault will be found
        int helloFaults = testFaults.stream().filter(e -> e.contains("An exception was thrown at Hello service")).collect(Collectors.toList()).size();
        assertEquals(40, helloFaults);
    }

    @DisplayName("Assert the exception 'book not found' was not found")
    @Test
    @Order(4)
    public void testBookNotFound() {
        // Assert that the "book not found" exception was not found.
        int bookNotFound = testFaults.stream().filter(e -> e.contains("Retrieved value is null. Book was not found")).collect(Collectors.toList()).size();
        assertEquals(0, bookNotFound);
    }

    @DisplayName("Assert all faults were deserialization faults or from the hello service")
    @Test
    @Order(5)
    public void testNumDeserializationAndHelloFaults() {
        // Out of 553 executions, 208 were deserialization faults
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // 40 iterations mutated the key "title" and therefore caused a fault in the hello service
        int helloFaults = testFaults.stream().filter(e -> e.contains("An exception was thrown at Hello service")).collect(Collectors.toList()).size();

        // Total faults should be 208 + 40 = 248 faults
        // This shows that only 248 / 553 = 44.8% of the executions actually caused faults
        // The rest of the executions were successful, although a bit was flipped
        assertEquals(248, deserializationFaults + helloFaults);

        // All faults should be either deserialization faults or from the hello service
        assertEquals(testFaults.size(), deserializationFaults + helloFaults);
    }

}
