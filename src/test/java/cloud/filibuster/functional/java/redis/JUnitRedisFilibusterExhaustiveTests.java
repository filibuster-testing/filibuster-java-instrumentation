package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.RedisExhaustiveAnalysisConfigurationFile;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterExhaustiveTests extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;

    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static int numberOfTestExecutions = 0;
    private static final HashMap<Class<?>, String[][]> allowedExceptions = new HashMap<>();

    @BeforeAll
    public static void primeCacheBeforeAll() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
    }

    @BeforeEach
    public void primeCacheBeforeEach() {
        statefulRedisConnection.sync().set(key, value);
    }

    @AfterEach
    public void primeCacheAfterEach() {
        statefulRedisConnection.sync().flushall();
    }

    static {
        allowedExceptions.put(RedisCommandTimeoutException.class, new String[][]{
                {"get", "Command timed out after 100 millisecond(s)"},
                {"hgetall", "Command timed out after 100 millisecond(s)"},
                {"hset", "Command timed out after 100 millisecond(s)"}});
        allowedExceptions.put(RedisConnectionException.class, new String[][]{{"sync", "Connection closed prematurely"}});
        allowedExceptions.put(RedisBusyException.class, new String[][]{
                {"flushall", "BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE"},
                {"flushdb", "BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE"}});
        allowedExceptions.put(RedisCommandExecutionException.class, new String[][]{
                {"hgetall", "WRONGTYPE Operation against a key holding the wrong kind of value"},
                {"hset", "WRONGTYPE Operation against a key holding the wrong kind of value"}});

    }

    @DisplayName("Exhaustive Redis fault injections")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisExhaustiveAnalysisConfigurationFile.class)
    public void testRedisSyncGetExhaustiveTests() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);

            // Test RedisConnectionException
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

            // Test RedisCommandTimeoutException
            assertEquals(value, myRedisCommands.get(key));

            // Test RedisBusyException
            myRedisCommands.flushall();
            myRedisCommands.flushdb();
            assertNull(myRedisCommands.get(key));

            // Test RedisCommandExecutionException
            myRedisCommands.hset(key, key, value);
            myRedisCommands.hgetall(key);

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());
            boolean found = false;

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnService(REDIS_MODULE_NAME), "Fault was not injected on the Redis module: " + t);

            String[][] exceptionsInfoList = allowedExceptions.get(t.getClass());
            if (exceptionsInfoList != null) {
                for (String[] exceptionInfo : exceptionsInfoList) {
                    if (wasFaultInjectedOnMethod(REDIS_MODULE_NAME, exceptionInfo[0])) {
                        found = true;
                        assertEquals(exceptionInfo[1], t.getMessage(), "Unexpected fault message" + t);
                        break;
                    }
                }
                if (!found) {
                    throw new AssertionFailedError("Injected fault details did not match the predefined criteria: " + t);
                }
            } else {
                throw new AssertionFailedError("Injected fault could not be found: " + t);
            }
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(10, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of Filibuster exceptions.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(allowedExceptions.size(), testExceptionsThrown.size());
    }

}
