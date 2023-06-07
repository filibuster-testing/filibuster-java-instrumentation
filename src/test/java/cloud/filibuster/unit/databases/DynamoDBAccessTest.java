package cloud.filibuster.unit.databases;

import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.DynamoDBClientService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamoDBAccessTest extends JUnitAnnotationBaseTest {

    private static DynamoDbClient dynamoDbClient;


    @BeforeAll
    public static void beforeAll() {
        dynamoDbClient = DynamoDBClientService.getInstance().dynamoDbClient;
    }

    @DisplayName("Basic test for DynamoDB access")
    @Test
    public void testBasicDynamoDBAccess() {
        int initTableSize = dynamoDbClient.listTables().tableNames().size();
        createNewTable(dynamoDbClient, "testTable1", "attr1");
        createNewTable(dynamoDbClient, "testTable2", "attr2");
        assertEquals(initTableSize + 2, dynamoDbClient.listTables().tableNames().size());
    }

    private static void createNewTable(DynamoDbClient dynamoDbClient, String tableName, String attr) {
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
