package cloud.filibuster.functional.java.redis;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.RedisExhaustiveAnalysisConfigurationFile;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
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
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitRedisFilibusterExhaustiveCoreExceptionsTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;

    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static int numberOfTestExecutions = 0;
    // Map in the format <ExceptionClass -> (<ClassNameOfMethod -> List(MethodName)>, ExceptionMessage)>.
    // The ClassNameOfMethod and MethodName are used to check if the fault was injected in the correct method and service.
    private static final Map<Class<?>, Entry<Map<String, List<String>>, String>> allowedExceptions = new HashMap<>();

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
                new SimpleEntry<>(ImmutableMap.of("io.lettuce.core.api.sync.RedisStringCommands", Collections.singletonList("get"),
                        "io.lettuce.core.api.async.RedisStringAsyncCommands", ImmutableList.of("get", "set"),
                        "io.lettuce.core.api.sync.RedisHashCommands", ImmutableList.of("hgetall", "hset")), "Command timed out after 100 millisecond(s)"));

        allowedExceptions.put(RedisBusyException.class,
                new SimpleEntry<>(ImmutableMap.of("io.lettuce.core.api.sync.RedisServerCommands", ImmutableList.of("flushall", "flushdb")),
                        "BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE"));

        allowedExceptions.put(RedisCommandExecutionException.class,
                new SimpleEntry<>(ImmutableMap.of("io.lettuce.core.api.sync.RedisHashCommands", ImmutableList.of("hgetall", "hset")),
                        "WRONGTYPE Operation against a key holding the wrong kind of value"));

        allowedExceptions.put(RedisCommandInterruptedException.class,
                new SimpleEntry<>(ImmutableMap.of("io.lettuce.core.RedisFuture", Collections.singletonList("await")),
                        "Command interrupted"));

    }

    @DisplayName("Exhaustive Core Redis fault injections")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = RedisExhaustiveAnalysisConfigurationFile.class, suppressCombinations = true)
    public void testRedisExhaustiveCoreTests() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = DynamicProxyInterceptor.createInterceptor(statefulRedisConnection, redisConnectionString);

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

            RedisAsyncCommands<String, String> myRedisAsyncCommands = myStatefulRedisConnection.async();
            RedisFuture<String> setResult = myRedisAsyncCommands.set(key, value);
            RedisFuture<String> getResult = myRedisAsyncCommands.get(key);

            // Test RedisCommandInterruptedException
            setResult.await(10, java.util.concurrent.TimeUnit.SECONDS);
            getResult.await(10, java.util.concurrent.TimeUnit.SECONDS);

            // Test RedisCommandTimeoutException
            setResult.get();
            getResult.get();

            assertFalse(wasFaultInjected());
        } catch (@SuppressWarnings("InterruptedExceptionSwallowed") Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            // Assert that a fault was injected
            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);

            Entry<Map<String, List<String>>, String> classMethodsMessageTuple = allowedExceptions.get(t.getClass());

            if (classMethodsMessageTuple != null) {
                String expectedExceptionMessage = classMethodsMessageTuple.getValue();
                // Assert that the exception message matches the expected one
                assertEquals(expectedExceptionMessage, t.getMessage(), "Unexpected fault message: " + t);

                Map<String, List<String>> classMethodsMap = classMethodsMessageTuple.getKey();
                boolean injectedMethodFound = false;

                for (Entry<String, List<String>> mapEntry : classMethodsMap.entrySet()) {
                    String className = mapEntry.getKey();
                    List<String> methodNames = mapEntry.getValue();

                    if (methodNames.stream().anyMatch(method -> wasFaultInjectedOnMethod(className + "/" + method))) {
                        assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService(className), "Expected FilibusterUnsupportedAPIException to be thrown");
                        injectedMethodFound = true;
                        break;
                    }
                }

                // Assert that the fault was injected on one of the expected methods of the given class
                if (!injectedMethodFound) {
                    throw new AssertionFailedError("Fault was not injected on any of the expected methods: " + t);
                }
            } else {
                throw new AssertionFailedError("Injected fault was not defined for this test: " + t);
            }
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(13, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of Filibuster exceptions.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(allowedExceptions.size(), testExceptionsThrown.size());
    }

}
