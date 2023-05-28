package cloud.filibuster.functional.java.dynamodb;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.DynamoDBClientService;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.cockroachdb.CockroachClientService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import static cloud.filibuster.instrumentation.Constants.DYNAMO_MODULE_NAME;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitDynamoDBTest extends JUnitAnnotationBaseTest {

    private static DynamoDbClient dynamoDbClient;
    private static String connectionString;


    @BeforeAll
    public static void beforeAll() {
        dynamoDbClient = DynamoDBClientService.getInstance().dynamoDbClient;
        connectionString = CockroachClientService.getInstance().connectionString;
    }

    @DisplayName("Inject basic exception in DynamoDB")
    @Order(1)
    @Test
    // TODO: Add analysis config file
    public void testInterceptConnection() {
        try {
            DynamoDbClient interceptedClient = DynamicProxyInterceptor.createInterceptor(dynamoDbClient, connectionString, DYNAMO_MODULE_NAME);
            int initTableSize = interceptedClient.listTables().tableNames().size();
            createNewTable(interceptedClient, "testTable1", "attr1");
            createNewTable(interceptedClient, "testTable2", "attr2");
            assertEquals(initTableSize + 2, interceptedClient.listTables().tableNames().size());
            assertFalse(wasFaultInjected());
        } catch (Exception t) {
            throw t;
            // TODO: Do some stuff
        }
    }

    private void createNewTable(DynamoDbClient dynamoDbClient, String tableName, String attr) {
        dynamoDbClient.createTable(
                CreateTableRequest
                        .builder()
                        .keySchema(
                                KeySchemaElement.builder()
                                        .keyType(KeyType.HASH)
                                        .attributeName(attr)
                                        .build()
                        )
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName(attr)
                                        .attributeType(ScalarAttributeType.S)
                                        .build()
                        )
                        .provisionedThroughput(
                                ProvisionedThroughput
                                        .builder()
                                        .readCapacityUnits(100L)
                                        .writeCapacityUnits(100L)
                                        .build()
                        )
                        .tableName(tableName)
                        .build()
        );
    }

}
