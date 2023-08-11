package cloud.filibuster.functional.database.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisGatewayTransformerAnalysisConfigurationFile;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterGatewayTransformerTest extends JUnitAnnotationBaseTest {
    static final String stringKey = "stringKey";
    static final String stringValue = "stringValue";
    static final String booleanTrueKey = "booleanTrueKey";
    static final String booleanTrueValue = "true";
    static final String booleanFalseKey = "booleanFalseKey";
    static final String booleanFalseValue = "false";
    static final String jsonKey = "jsonKey";
    static final String jsonValue = new JSONObject().put("hello", "world").put("foo", "bar").toString();
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(stringKey, stringValue);
        statefulRedisConnection.sync().set(booleanTrueKey, booleanTrueValue);
        statefulRedisConnection.sync().set(booleanFalseKey, booleanFalseValue);
        statefulRedisConnection.sync().set(jsonKey, jsonValue);
    }

    @DisplayName("Tests Redis gateway transformer for Strings.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisGatewayTransformerAnalysisConfigurationFile.class)
    public void testRedisGatewayStringTransformation() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

            String returnVal = myRedisCommands.get(stringKey);
            assertEquals(stringValue, returnVal);

            returnVal = myRedisCommands.get(booleanFalseKey);
            assertEquals(booleanFalseValue, returnVal);

            returnVal = myRedisCommands.get(booleanTrueKey);
            assertEquals(booleanTrueValue, returnVal);

            returnVal = myRedisCommands.get(jsonKey);
            assertEquals(jsonValue, returnVal);

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // 1 for the original test and +1 for each character manipulation in the string stringValue + 1 for each boolean value + 1 for each JSON key/value pair
    // 1 + 13 + 1 + 1 + 2 = 18
    public void testNumExecutions() {
        assertEquals(stringValue.length() + 5, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of faults.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(stringValue.length() + 4, testExceptionsThrown.size());
    }

}
