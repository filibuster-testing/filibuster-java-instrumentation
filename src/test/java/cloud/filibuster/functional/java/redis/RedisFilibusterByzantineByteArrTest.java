package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.byzantine.redis.RedisSingleGetByteArrByzantineFaultAnalysisConfigurationFile;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisFilibusterByzantineByteArrTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final byte[] value = "example".getBytes(Charset.defaultCharset());
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private static int numberOfTestExecutions = 0;
    private final List<String> expectedValues = Arrays.asList("", "ThisIsATestString", "abcd", "1234!!", "-11", null);
    private static final Set<byte[]> actualValues = new LinkedHashSet<>();

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(key, value);
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - Byzantine byte array fault injection")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisSingleGetByteArrByzantineFaultAnalysisConfigurationFile.class)
    public void testRedisByzantineGet() {
        numberOfTestExecutions++;

        StatefulRedisConnection<String, byte[]> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
        RedisCommands<String, byte[]> myRedisCommands = myStatefulRedisConnection.sync();
        byte[] returnVal = myRedisCommands.get(key);

        if (!wasFaultInjected()) {
            assertArrayEquals(value, returnVal, "The value returned from Redis was not the expected value although no byzantine fault was injected.");
        } else {
            actualValues.add(returnVal);
            String returnValStr = returnVal == null ? null : new String(returnVal, Charset.defaultCharset());
            assertTrue(expectedValues.contains(returnValStr), "An unexpected value was returned: " + returnValStr);
            assertTrue(wasFaultInjectedOnJavaClassAndMethod("io.lettuce.core.api.sync.RedisStringCommands/get"), "Fault was not injected on the expected Redis method");
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // One execution for each expected value + 1 for the non-faulty execution
        assertEquals(expectedValues.size() + 1, numberOfTestExecutions);
    }

    @DisplayName("Verify whether all expected values were returned.")
    @Test
    @Order(2)
    public void testNumReturnValues() {
        assertEquals(expectedValues.size(), actualValues.size());
    }

}
