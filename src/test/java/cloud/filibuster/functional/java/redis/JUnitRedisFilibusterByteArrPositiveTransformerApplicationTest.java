package cloud.filibuster.functional.java.redis;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisTransformBitInByteArrAnalysisConfigurationFile;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.charset.Charset;
import java.util.ArrayList;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterByteArrPositiveTransformerApplicationTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static byte[] value;
    static JSONObject referenceJSONObject;
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private final static ArrayList<String> testExceptionsThrown = new ArrayList<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void primeCache() {
        referenceJSONObject = new JSONObject();
        referenceJSONObject.put("uni", "cmu");
        referenceJSONObject.put("course", "15440");
        referenceJSONObject.put("city", "pittsburgh");
        referenceJSONObject.put("state", "pa");

        value = referenceJSONObject.toString().getBytes(Charset.defaultCharset());

        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(key, value);
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Byte Array transformer faults.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAnalysisConfigurationFile.class,
            maxIterations = 550)
    public void testRedisByteArrPositiveTransformation() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, byte[]> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, byte[]> myRedisCommands = myStatefulRedisConnection.sync();

            // Get the value as byte[] from the cache
            byte[] returnVal = myRedisCommands.get(key);
            // Convert the byte[] to a string
            String sJsonObject = new String(returnVal, Charset.defaultCharset());
            // Convert the string to a JSONObject
            JSONObject returnJO = new JSONObject(sJsonObject);

            // Check if the JSONObject contains the key "uni"
            assertTrue(returnJO.has("uni"), "Expected key \"uni\" not found in JSONObject: " + returnJO);
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("io.lettuce.core.api.sync.RedisStringCommands"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnMethod("io.lettuce.core.api.sync.RedisStringCommands/get"), "Fault was not injected on the expected Redis method: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // 1 for the original test and +1 for each bit in the byte array
    public void testNumExecutions() {
        assertEquals(value.length * 8 + 1, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of faults.")
    @Test
    @Order(3)
    public void testTotalNumFaults() {
        // Faults causing deserialisation exceptions were injected on "{", "}", ":", "," and "\""
        // { = 1, } = 1, : = 4, , = 3, " = 16 (4 keys + 4 values, each with two quotes)
        // Total number of deserialisation faults = 1 + 1 + 4 + 3 + 16 = 25
        // Each fault is injected 8 times (once for each bit in the byte array) = 25 * 8 = 200 faults
        // +1 fault for unterminated string
        // The key "uni" has 3 chars, each char has 8 bits, so 3 * 8 = 24 faults
        // Total number of faults = 200 + 24 + 1 = 225
        assertEquals(225, testExceptionsThrown.size());
    }

    @DisplayName("Verify correct number of faults.")
    @Test
    @Order(4)
    public void testNumFaultsOnKey() {
        // The key "uni" has 3 chars, each char has 8 bits, so 3 * 8 = 24 faults
        int keyFaults = testExceptionsThrown.stream().filter(e -> e.contains("Expected key \"uni\" not found in JSONObject")).toList().size();
        assertEquals(24, keyFaults);
    }


}
