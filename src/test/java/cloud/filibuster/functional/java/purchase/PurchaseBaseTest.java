package cloud.filibuster.functional.java.purchase;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Metadata.setMetadataDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.TestScope.setTestScopeCounter;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;

public class PurchaseBaseTest {
    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    @BeforeAll
    public static void startRedis() {
        RedisClientService.getInstance().redisClient.connect().sync().set("purchased", "0");
    }

    @BeforeAll
    public static void setProperties() {
        setTestScopeCounter(true);
        setMetadataDigest(false);
    }

    @AfterAll
    public static void resetProperties() {
        setTestScopeCounter(false);
        setMetadataDigest(true);
    }

    public static final ManagedChannel API_CHANNEL = ManagedChannelBuilder
            .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
            .usePlaintext()
            .build();
}
