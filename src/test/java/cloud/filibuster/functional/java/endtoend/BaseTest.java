package cloud.filibuster.functional.java.endtoend;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;

public class BaseTest {
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

    public static final ManagedChannel API_CHANNEL = ManagedChannelBuilder
            .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
            .usePlaintext()
            .build();
}
