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

import java.util.HashMap;
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
    static JSONObject studentInfoJO = new JSONObject();
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static int numberOfTestExecutions = 0;
    private final static HashMap<String, Object> studentInfoMap = new HashMap<>();
    private final static HashMap<String, Object> courseInfoMap = new HashMap<>();


    @BeforeAll
    public static void primeCache() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
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

        statefulRedisConnection.sync().set(jsonKey, studentInfoJO.toString());
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
            assertEquals(studentInfoJO.toString(), returnVal);

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
        assertEquals(1 + getNumFaultsInJS() + stringValue.length() + 2, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of faults.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(getNumFaultsInJS() + stringValue.length() + 2, testExceptionsThrown.size());
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
