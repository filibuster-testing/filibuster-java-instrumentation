package cloud.filibuster.functional.java.redis;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.redis.RedisExhaustiveAnalysisConfigurationFile;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class JUnitRedisFilibusterExhaustiveCoreExceptionsTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;

    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static int numberOfTestExecutions = 0;
    private static final Map<Class<?>, Entry<List<String>, String>> allowedExceptions = new HashMap<>();

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
        allowedExceptions.put(RedisCommandTimeoutException.class,
                new AbstractMap.SimpleEntry<>(Arrays.asList("io.lettuce.core.api.sync.RedisStringCommands.get",
                        "io.lettuce.core.api.async.RedisStringAsyncCommands.get",
                        "io.lettuce.core.api.sync.RedisHashCommands.hgetall",
                        "io.lettuce.core.api.sync.RedisHashCommands.hset"), "Command timed out after 100 millisecond(s)"));

        allowedExceptions.put(RedisBusyException.class,
                new AbstractMap.SimpleEntry<>(Arrays.asList("io.lettuce.core.api.sync.RedisServerCommands.flushall",
                        "io.lettuce.core.api.sync.RedisServerCommands.flushdb"),
                        "BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE"));

        allowedExceptions.put(RedisCommandExecutionException.class,
                new AbstractMap.SimpleEntry<>(Arrays.asList("io.lettuce.core.api.sync.RedisHashCommands.hgetall",
                        "io.lettuce.core.api.sync.RedisHashCommands.hset"),
                        "WRONGTYPE Operation against a key holding the wrong kind of value"));

        allowedExceptions.put(RedisCommandInterruptedException.class,
                new AbstractMap.SimpleEntry<>(Collections.singletonList("io.lettuce.core.RedisFuture.await"),
                        "Command interrupted"));

    }

    @DisplayName("Exhaustive Core Redis fault injections")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisExhaustiveAnalysisConfigurationFile.class)
    public void testRedisExhaustiveCoreTests() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);

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

            // Test RedisCommandInterruptedException
            RedisAsyncCommands<String, String> myRedisAsyncCommands = myStatefulRedisConnection.async();
            myRedisAsyncCommands.get(key).await(10, java.util.concurrent.TimeUnit.SECONDS);

            assertFalse(wasFaultInjected());
        } catch (@SuppressWarnings("InterruptedExceptionSwallowed") Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnService(REDIS_MODULE_NAME), "Fault was not injected on the expected Redis module: " + t);

            Entry<List<String>, String> methodsMessagePair = allowedExceptions.get(t.getClass());
            if (methodsMessagePair != null) {
                assertEquals(methodsMessagePair.getValue(), t.getMessage(), "Unexpected fault message: " + t);
                assertTrue(methodsMessagePair.getKey().stream().anyMatch(method ->
                                wasFaultInjectedOnMethod(REDIS_MODULE_NAME, method)),
                        "Fault was not injected on any of the expected methods (" + methodsMessagePair.getKey() + "): " + t);
            } else {
                throw new AssertionFailedError("Injected fault was not defined for this test: " + t);
            }
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(11, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of Filibuster exceptions.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(allowedExceptions.size(), testExceptionsThrown.size());
    }

}
