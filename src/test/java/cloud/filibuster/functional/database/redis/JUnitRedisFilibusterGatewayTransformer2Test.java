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
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterGatewayTransformer2Test extends JUnitAnnotationBaseTest {
    static final String jsonKey = "jsonKey";
    static String jsonValue;
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;

        JSONObject nestedJO = new JSONObject().put("nested_key", "nested_value");
        jsonValue = new JSONObject().put("hello", "world")
                .put("foo", "bar")
                .put("bool", "true")
                .put("nested", nestedJO)
                .toString();

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

            String returnVal = myRedisCommands.get(jsonKey);
            assertEquals(jsonValue, returnVal);

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
        }
    }

//    @DisplayName("Verify correct number of test executions.")
//    @Test
//    @Order(2)
//    // TODO
//    public void testNumExecutions() {
//        assertEquals(10, numberOfTestExecutions);
//    }
//
//    @DisplayName("Verify correct number of faults.")
//    @Test
//    @Order(3)
//    // TODO
//    public void testNumExceptions() {
//        assertEquals(10, testExceptionsThrown.size());
//    }

}
