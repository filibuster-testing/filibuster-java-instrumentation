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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterByteArrTransformerTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final byte[] value = "example".getBytes(Charset.defaultCharset());
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private final static ArrayList<String> testExceptionsThrown = new ArrayList<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(key, value);
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Byte Array transformer faults.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisTransformBitInByteArrAnalysisConfigurationFile.class)
    public void testRedisByteArrTransformation() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, byte[]> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, byte[]> myRedisCommands = myStatefulRedisConnection.sync();
            byte[] returnVal = myRedisCommands.get(key);
            assertArrayEquals(value, returnVal);
            assertFalse(wasFaultInjected());
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
    // 1 fault per bit per byte in the byte array
    public void testNumFaults() {
        assertEquals(value.length * 8, testExceptionsThrown.size());
    }

}
