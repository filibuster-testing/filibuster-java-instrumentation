package cloud.filibuster.functional.database.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisGatewayTransformerAnalysisConfigurationFile;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterByteArrGatewayTransformerTest extends JUnitAnnotationBaseTest {
    static final String stringKey = "stringKey";
    static final byte[] stringValue = "stringValue".getBytes(Charset.defaultCharset());
    static final String booleanTrueKey = "booleanTrueKey";
    static final byte[] booleanTrueValue = "true".getBytes(Charset.defaultCharset());
    static final String booleanFalseKey = "booleanFalseKey";
    static final byte[] booleanFalseValue = "false".getBytes(Charset.defaultCharset());
    static final String jsonKey = "jsonKey";
    static JSONObject studentInfoJO = new JSONObject();
    static StatefulRedisConnection<String, byte[]> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static int numberOfTestExecutions = 0;
    private final static HashMap<String, Object> studentInfoMap = new HashMap<>();
    private final static HashMap<String, Object> courseInfoMap = new HashMap<>();


    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect(RedisCodec.of(new StringCodec(), new ByteArrayCodec()));
        redisConnectionString = RedisClientService.getInstance().connectionString;
        statefulRedisConnection.sync().set(stringKey, stringValue);
        statefulRedisConnection.sync().set(booleanTrueKey, booleanTrueValue);
        statefulRedisConnection.sync().set(booleanFalseKey, booleanFalseValue);

        studentInfoMap.put("name", "Alice");
        studentInfoMap.put("age", 20);
        studentInfoMap.put("isEnrolled", true);

        courseInfoMap.put("course_name", "Distributed Systems");
        courseInfoMap.put("courseNumber", "CS 425");
        courseInfoMap.put("isGraduateCourse", false);
        courseInfoMap.put("passed", true);
        courseInfoMap.put("grade", "A");

        JSONObject courseInfoJO = new JSONObject();
        buildJOFromMap(courseInfoJO, courseInfoMap);

        buildJOFromMap(studentInfoJO, studentInfoMap);

        studentInfoJO.put("last_course", courseInfoJO);

        statefulRedisConnection.sync().set(jsonKey, studentInfoJO.toString().getBytes(Charset.defaultCharset()));
    }

    @DisplayName("Tests Redis gateway transformer for byte arrays.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisGatewayTransformerAnalysisConfigurationFile.class)
    public void testRedisGatewayByteArrTransformation() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, byte[]> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);
            RedisCommands<String, byte[]> myRedisCommands = myStatefulRedisConnection.sync();

            byte[] returnVal = myRedisCommands.get(stringKey);
            assertArrayEquals(stringValue, returnVal);

            returnVal = myRedisCommands.get(booleanFalseKey);
            assertArrayEquals(booleanFalseValue, returnVal);

            returnVal = myRedisCommands.get(booleanTrueKey);
            assertArrayEquals(booleanTrueValue, returnVal);

            returnVal = myRedisCommands.get(jsonKey);
            assertArrayEquals(studentInfoJO.toString().getBytes(Charset.defaultCharset()), returnVal);

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // 1 for the original test and +1 for each character manipulation in the string stringValue + 1 for each boolean value + faults in JSONObject
    public void testNumExecutions() {
        assertEquals(1 + getNumFaultsInJS() + stringValue.length + 2, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of faults.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(getNumFaultsInJS() + stringValue.length + 2, testExceptionsThrown.size());
    }

    private static void buildJOFromMap(JSONObject jo, HashMap<String, Object> map) {
        for (String key : map.keySet()) {
            jo.put(key, map.get(key));
        }
    }

    private static int getNumFaultsInJS() {
        int numFaults = 0;
        HashMap<String, Object> allMaps = new HashMap<>();
        allMaps.putAll(studentInfoMap);
        allMaps.putAll(courseInfoMap);

        for (Object value : allMaps.values()) {
            if (value.equals(true) || value.equals(false)) {
                ++numFaults;
            } else {
                numFaults += String.valueOf(value).length();
            }
        }
        return numFaults;
    }
}
