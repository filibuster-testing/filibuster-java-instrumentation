package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.annotation.Nullable;
import java.net.URI;

public class DynamoDBClientService {
    public DynamoDbClient dynamoDbClient;
    public String connectionString;

    @Nullable
    private static DynamoDBClientService single_instance = null;

    @SuppressWarnings("resource")
    private DynamoDBClientService() {
        GenericContainer<?> dynamoContainer = new GenericContainer<>(
                DockerImageName.parse("amazon/dynamodb-local")
        ).withExposedPorts(8000);
        dynamoContainer.start();
        connectionString = String.format("http://localhost:%s", dynamoContainer.getFirstMappedPort().toString());
        dynamoDbClient = getDynamoClient(connectionString);
    }

    private DynamoDbClient getDynamoClient(String connectionString) {
        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(connectionString))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("FAKE", "FAKE")))
                .build();
    }

    // Static method to create instance of Singleton class
    public static synchronized DynamoDBClientService getInstance() {
        if (single_instance == null) {
            single_instance = new DynamoDBClientService();
        }

        return single_instance;
    }
}
