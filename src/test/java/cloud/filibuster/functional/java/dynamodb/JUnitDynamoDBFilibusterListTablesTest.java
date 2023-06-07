package cloud.filibuster.functional.java.dynamodb;

import cloud.filibuster.exceptions.filibuster.FilibusterUnsupportedAPIException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.DynamoDBClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.dynamodb.DynamoSingleFaultLimitExceededExceptionAnalysisConfigurationFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JUnitDynamoDBFilibusterListTablesTest extends JUnitAnnotationBaseTest {

    private static DynamoDbClient dynamoDbClient;
    private static String connectionString;
    private static int numberOfTestExecutions = 0;
    private final static Set<String> testExceptionsThrown = new HashSet<>();


    @BeforeAll
    public static void beforeAll() {
        dynamoDbClient = DynamoDBClientService.getInstance().dynamoDbClient;
        connectionString = DynamoDBClientService.getInstance().connectionString;
    }

    @DisplayName("Test basic fault injection for DynamoDB")
    @TestWithFilibuster(analysisConfigurationFile = DynamoSingleFaultLimitExceededExceptionAnalysisConfigurationFile.class)
    public void testBasicDynamoDBFaultInjection() {
        try {
            numberOfTestExecutions++;

            DynamoDbClient interceptedClient = DynamicProxyInterceptor.createInterceptor(dynamoDbClient, connectionString);
            int initTableSize = interceptedClient.listTables().tableNames().size();
            assertEquals(0, initTableSize);
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                testExceptionsThrown.add(t.getMessage());

                assertThrows(FilibusterUnsupportedAPIException.class, () -> wasFaultInjectedOnService("software.amazon.awssdk.services.dynamodb.DynamoDbClient"), "Expected FilibusterUnsupportedAPIException to be thrown: " + t);
                assertTrue(wasFaultInjectedOnMethod("software.amazon.awssdk.services.dynamodb.DynamoDbClient/listTables"), "Fault was not injected on the expected DynamoDB method");
                String expectedErrorMessage = "Throughput exceeds the current throughput limit";
                assertTrue(t.getMessage().contains(expectedErrorMessage), "Unexpected return error message for injected byzantine fault");
            } else {
                throw t;
            }
        }
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // 1 fault free execution + 1 fault injection
        assertEquals(2, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(1, testExceptionsThrown.size());
    }


}
