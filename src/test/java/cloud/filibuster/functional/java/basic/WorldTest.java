package cloud.filibuster.functional.java.basic;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
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
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static int invocationCount = 0;

    @TestWithFilibuster
    public void testWorld() {
        invocationCount++;

        try {
            APIServiceGrpc.APIServiceBlockingStub apiServiceBlockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Chris").build();
            Hello.HelloReply reply = apiServiceBlockingStub.world(request);
            assertEquals("Hello!", reply.getMessage());

            if (invocationCount == 1) {
                assertFalse(wasFaultInjected());
            } else {
                assertTrue(wasFaultInjected());
            }
        } catch (StatusRuntimeException sre) {
            assertTrue(wasFaultInjected());
        }
    }
}
