package cloud.filibuster.functional.java.redis;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTransformBitInByteArrAndByzantineAnalysisConfigurationFile;
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
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterGetBookTest extends JUnitAnnotationBaseTest {
    static JSONObject referenceBook;
    static byte[] bookBytes;
    static String bookId = "abc123";
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private final static ArrayList<String> testFaults = new ArrayList<>();
    private static int numberOfTestExecutions = 0;
    private static ManagedChannel apiChannel;

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

    @DisplayName("Tests whether a book can be retrieved from Redis - Inject transformer faults where random bits are flipped in the byte array.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAndByzantineAnalysisConfigurationFile.class,
            maxIterations = 1000)
    public void testGetBookFromRedis() {
        numberOfTestExecutions++;

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.GetBookRequest bookRequest = Hello.GetBookRequest.newBuilder().setBookId(bookId).build();
            Hello.GetBookResponse reply = blockingStub.getBook(bookRequest);
            assertNotNull(reply.getBook());
        } catch (Throwable t) {
            testFaults.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("io.lettuce.core.api.sync.RedisStringCommands"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/get"), "Fault was not injected on the expected Redis method: " + t);

        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // 1 for the reference execution, +1 for the byzantine faults (injecting null), and +1 for each bit in the byte array
    // => 1 + 1 + bookBytes.length * 8 = 1 + 1 + 69 * 8 = 554
    public void testNumExecutions() {
        assertEquals(1 + 1 + bookBytes.length * 8, numberOfTestExecutions);
    }

    @DisplayName("Assert all faults that occurred were deserialization faults")
    @Test
    @Order(3)
    public void testNumDeserializationFaults() {
        // In this scenario, the bit transformations should only cause deserialization faults
        // Out of 554 executions, 208 caused deserialization faults
        // This shows that only 208 / 554 = 37.5% of the executions actually caused faults
        // The rest of the executions were successful, although a bit was flipped
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // All faults should be deserialization faults
        assertEquals(208, deserializationFaults);
    }

    @DisplayName("Assert the fault at the hello service was found once")
    @Test
    @Order(4)
    public void testHelloFaultNotFound() {
        // The bit transformations should only cause deserialization faults
        // Out of 554 executions, only one execution, in which the Byzantine value "null" was injected,
        // leads us to discover the fault at the hello service
        int helloFaults = testFaults.stream().filter(e -> e.contains("An exception was thrown at Hello service")).collect(Collectors.toList()).size();
        assertEquals(1, helloFaults);
    }

    @DisplayName("Assert all faults were either deserialization faults, Byzantine faults or from the hello service")
    @Test
    @Order(5)
    public void testNumDeserializationAndHelloAndByzantineFaults() {
        // Out of 554 executions, 208 were deserialization faults
        int deserializationFaults = testFaults.stream().filter(e -> e.contains("Error deserializing")).collect(Collectors.toList()).size();

        // 1 iteration injected a null value, which caused a fault at the hello service
        int helloFaults = testFaults.stream().filter(e -> e.contains("An exception was thrown at Hello service")).collect(Collectors.toList()).size();

        // Total faults should be 208 + 1 = 209 faults
        // This shows that only 249 / 554 = 44.9% of the executions actually caused faults
        // The rest of the executions were successful, although a bit was flipped
        assertEquals(209, deserializationFaults + helloFaults);

        // All faults should be either deserialization faults, or from the byzantine fault causing the hello service to throw an exception
        assertEquals(testFaults.size(), deserializationFaults + helloFaults);
    }

}
