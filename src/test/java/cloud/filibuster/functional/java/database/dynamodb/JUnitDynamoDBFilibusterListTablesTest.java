package cloud.filibuster.functional.java.database.dynamodb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.DynamoDBClientService;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.db.dynamodb.DynamoDBAnalysisConfigurationFile;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjectedOnJavaClassAndMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JUnitDynamoDBFilibusterListTablesTest extends JUnitAnnotationBaseTest {

    private static DynamoDbClient dynamoDbClient;
    private static String connectionString;
    private static int numberOfTestExecutions = 0;
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static final String tableName = "test_table";
    private static final Map<Class<?>, Map.Entry<Map<String, List<String>>, String>> allowedExceptions = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
        dynamoDbClient = DynamoDBClientService.getInstance().dynamoDbClient;
        connectionString = DynamoDBClientService.getInstance().connectionString;
    }

    @AfterEach
    public void afterEach() {
        // Drop all tables
        List<String> tableNames = dynamoDbClient.listTables().tableNames();
        for (String tableName : tableNames) {
            deleteTable(dynamoDbClient, tableName);
        }
    }

    static {
        allowedExceptions.put(RequestLimitExceededException.class,
                new AbstractMap.SimpleEntry<>(
                        ImmutableMap.of("software.amazon.awssdk.services.dynamodb.DynamoDbClient", ImmutableList.of("listTables", "createTable", "deleteTable", "putItem", "getItem")),
                        "Throughput exceeds the current throughput limit for your account. Please contact AWS Support at " +
                                "https://aws.amazon.com/support request a limit increase"));

        allowedExceptions.put(SdkClientException.class,
                new AbstractMap.SimpleEntry<>(
                        ImmutableMap.of("software.amazon.awssdk.services.dynamodb.DynamoDbClient", ImmutableList.of("listTables", "createTable", "deleteTable", "putItem", "getItem")),
                        "Unable to load region information from any provider in the chain"));

        allowedExceptions.put(DynamoDbException.class,
                new AbstractMap.SimpleEntry<>(
                        ImmutableMap.of("software.amazon.awssdk.services.dynamodb.DynamoDbClient", ImmutableList.of("listTables", "createTable", "deleteTable", "putItem", "getItem")),
                        "Validation errors detected"));

        allowedExceptions.put(AwsServiceException.class,
                new AbstractMap.SimpleEntry<>(
                        ImmutableMap.of("software.amazon.awssdk.services.dynamodb.DynamoDbClient", ImmutableList.of("listTables", "createTable", "deleteTable", "putItem", "getItem")),
                        ""));
    }

    @DisplayName("Test basic fault injection for DynamoDB")
    @TestWithFilibuster(analysisConfigurationFile = DynamoDBAnalysisConfigurationFile.class)
    public void testBasicDynamoDBFaultInjection() {
        try {
            numberOfTestExecutions++;

            DynamoDbClient interceptedClient = DynamicProxyInterceptor.createInterceptor(dynamoDbClient, connectionString);

            // Create table
            createTable(interceptedClient, tableName);
            int tableSize = interceptedClient.listTables().tableNames().size();
            assertEquals(1, tableSize, "Table was not created");

            // Sample data for the item
            String userId = "user123";
            String userName = "John Doe";
            int userAge = 30;

            // Put item into DynamoDB
            putItem(dynamoDbClient, tableName, userId, userName, userAge);

            // Get item from DynamoDB
            Map<String, AttributeValue> item = getItem(dynamoDbClient, tableName, userId);
            assertEquals(userId, item.get("userId").s());
            assertEquals(userName, item.get("userName").s());
            assertEquals(String.valueOf(userAge), item.get("userAge").n());

            // Delete table
            deleteTable(dynamoDbClient, tableName);
            tableSize = interceptedClient.listTables().tableNames().size();
            assertEquals(0, tableSize, "Table was not deleted");

            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            // Assert that a fault was injected
            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);

            Map.Entry<Map<String, List<String>>, String> classMethodsMessageTuple = allowedExceptions.get(t.getClass());

            if (classMethodsMessageTuple != null) {
                String expectedExceptionMessage = classMethodsMessageTuple.getValue();
                // Assert that the exception message matches the expected one
                assertEquals(expectedExceptionMessage, t.getMessage(), "Unexpected fault message: " + t);

                Map<String, List<String>> classMethodsMap = classMethodsMessageTuple.getKey();
                boolean injectedMethodFound = false;

                for (Map.Entry<String, List<String>> mapEntry : classMethodsMap.entrySet()) {
                    String className = mapEntry.getKey();
                    List<String> methodNames = mapEntry.getValue();

                    if (methodNames.stream().anyMatch(method -> wasFaultInjectedOnJavaClassAndMethod(className + "/" + method))) {
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

    private static void putItem(DynamoDbClient dynamoDbClient, String tableName, String userId, String userName, int userAge) {
        // Define the attributes of the item to be put
        // Here, 'userId', 'userName', and 'userAge' are attribute names
        // and their values are defined using AttributeValue.builder().s() or AttributeValue.builder().n()
        // based on their data types.
        Map<String, AttributeValue> valueMap = new HashMap<>();
        valueMap.put("userId", AttributeValue.builder().s(userId).build());
        valueMap.put("userName", AttributeValue.builder().s(userName).build());
        valueMap.put("userAge", AttributeValue.builder().n(String.valueOf(userAge)).build());

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(
                        valueMap
                )
                .build();

        dynamoDbClient.putItem(putItemRequest);
    }

    private static Map<String, AttributeValue> getItem(DynamoDbClient dynamoDbClient, String tableName, String userId) {
        // Define the primary key for the item to be retrieved
        // Here, 'userId' is the primary key attribute name, and its value is provided.
        Map<String, AttributeValue> valueMap = new HashMap<>();
        valueMap.put("userId", AttributeValue.builder().s(userId).build());

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(
                        valueMap
                )
                .build();

        GetItemResponse response = dynamoDbClient.getItem(getItemRequest);
        return response.item();
    }

    private static void createTable(DynamoDbClient dynamoDbClient, String tableName) {
        // Define the key attributes for the table (primary key)
        String partitionKeyName = "userId"; // Partition key attribute name
        ScalarAttributeType partitionKeyType = ScalarAttributeType.S; // 'S' for String, 'N' for Number

        // Specify the provisioned throughput for the table (read and write capacity units)
        long readCapacityUnits = 5;
        long writeCapacityUnits = 5;

        // Create the CreateTableRequest
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(partitionKeyName)
                                .keyType(KeyType.HASH)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(partitionKeyName)
                                .attributeType(partitionKeyType)
                                .build()
                )
                .provisionedThroughput(
                        ProvisionedThroughput.builder()
                                .readCapacityUnits(readCapacityUnits)
                                .writeCapacityUnits(writeCapacityUnits)
                                .build()
                )
                .build();

        // Execute the CreateTable operation
        dynamoDbClient.createTable(createTableRequest);
    }

    private static void deleteTable(DynamoDbClient dynamoDbClient, String tableName) {
        DeleteTableRequest deleteTableRequest = DeleteTableRequest.builder()
                .tableName(tableName)
                .build();

        dynamoDbClient.deleteTable(deleteTableRequest);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // 1 fault free execution + 12 executions with injected faults
        assertEquals(13, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(allowedExceptions.size(), testExceptionsThrown.size());
    }


}
