package cloud.filibuster.functional.java.http;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Metadata.setMetadataDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.TestScope.setTestScopeCounter;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorldTest {
    public static ManagedChannel apiChannel;

    @BeforeAll
    public static void startAllServicesAndSetProperties() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
        startExternalServerAndWaitUntilAvailable();

        setMetadataDigest(false);
        setTestScopeCounter(true);

        apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();
    }

    @AfterAll
    public static void stopAllServicesAndUnsetProperties() throws InterruptedException {
        apiChannel.shutdownNow();
        apiChannel.awaitTermination(1000, TimeUnit.SECONDS);

        setMetadataDigest(true);
        setTestScopeCounter(false);

        stopExternalServerAndWaitUntilUnavailable();
        stopWorldServerAndWaitUntilUnavailable();
        stopHelloServerAndWaitUntilUnavailable();
        stopAPIServerAndWaitUntilUnavailable();
    }

    @TestWithFilibuster
    public void testWorld() {
        APIServiceGrpc.APIServiceBlockingStub apiServiceBlockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Chris").build();
        Hello.HelloReply reply = apiServiceBlockingStub.world(request);
        assertEquals("Hello!", reply.getMessage());
    }
}