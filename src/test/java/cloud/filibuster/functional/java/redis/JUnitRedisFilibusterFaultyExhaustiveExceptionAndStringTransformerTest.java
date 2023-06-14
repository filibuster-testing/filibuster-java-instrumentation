package cloud.filibuster.functional.java.redis;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.redis.FaultyRedisTransformStringAnalysisConfigurationFile;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class JUnitRedisFilibusterFaultyExhaustiveExceptionAndStringTransformerTest extends JUnitAnnotationBaseTest {
    static final String key = "test";
    static final String value = "example";
    static StatefulRedisConnection<String, String> statefulRedisConnection;
    static String redisConnectionString;
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    @BeforeAll
    public static void beforeAll() {
        statefulRedisConnection = RedisClientService.getInstance().redisClient.connect();
        redisConnectionString = RedisClientService.getInstance().connectionString;
    }

    @DisplayName("Tests whether Redis sync interceptor can read from existing key - String transformer BFI with Faulty Accumulator.")
    @Order(1)
    @TestWithFilibuster(analysisConfigurationFile = FaultyRedisTransformStringAnalysisConfigurationFile.class)
    public void testRedisStringBFIWithFaultyAccumulator() {
        try {
            numberOfTestExecutions++;

            StatefulRedisConnection<String, String> myStatefulRedisConnection = new RedisInterceptorFactory<>(statefulRedisConnection, redisConnectionString).getProxy(StatefulRedisConnection.class);
            RedisCommands<String, String> myRedisCommands = myStatefulRedisConnection.sync();

            myRedisCommands.set(key, value);
            String returnVal = myRedisCommands.get(key);
            assertEquals(value, returnVal);

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("io.lettuce.core.api.sync.RedisStringCommands"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
            assertTrue(wasFaultInjectedOnMethod("io.lettuce.core.api.sync.RedisStringCommands/get") ||
                    wasFaultInjectedOnMethod("io.lettuce.core.api.sync.RedisStringCommands/set"),
                    "Fault was not injected on the expected Redis method: " + t);
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // The reference execution (test #1) determines that the get-method was called. The get-method returns a valid value
        // (i.e., not null or the empty string). Test #1 schedules BFI Test #2. No accumulator has been created till this point.
        // When test #2 is executed, an accumulator is created for the first time with idx = 0. Test #2 schedules test #3.
        // Now the fault object contains an accumulator with idx = 0.
        // When test #3 is executed, it creates a new faulty accumulator which also has idx = 0.
        // Test #3 tries to schedule test #4 with an accumulator also having idx = 0.
        // Since the fault object is now repeated, abstractIsExploredExecution is set to true and test #4 is not scheduled.
        assertEquals(3, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of unique injected faults.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        // Only 1 exception from mutating char 0 of "example" to "X"
        assertEquals(1, testExceptionsThrown.size());
    }

}
