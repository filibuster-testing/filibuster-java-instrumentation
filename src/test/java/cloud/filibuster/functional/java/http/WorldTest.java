package cloud.filibuster.functional.java.http;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import com.linecorp.armeria.common.HttpMethod;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;
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
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.protocols.HttpAssertions.wasFaultInjectedOnHttpMethod;
import static cloud.filibuster.junit.assertions.protocols.HttpAssertions.wasFaultInjectedOnHttpRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @TestWithFilibuster
    public void testWorld() {
        try {
            APIServiceGrpc.APIServiceBlockingStub apiServiceBlockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Chris").build();
            Hello.HelloReply reply = apiServiceBlockingStub.world(request);
            assertEquals("Hello!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (StatusRuntimeException sre) {
            boolean expected = false;

            if (wasFaultInjectedOnHttpMethod(HttpMethod.GET, "http://0.0.0.0:5002/world")) {
                expected = true;
                assertTrue(sre.getMessage().contains("First RPC request to /world failed!"));
            }

            if (wasFaultInjectedOnHttpMethod(HttpMethod.GET, "http://0.0.0.0:5003")) {
                expected = true;
                assertTrue(sre.getMessage().contains("First RPC request to /world failed!"));
            }

            if (wasFaultInjectedOnHttpMethod(HttpMethod.GET, "http://0.0.0.0:.*/external-post")) {
                expected = true;
                assertTrue(sre.getMessage().contains("Second RPC request to /world failed!"));
            }

            if (wasFaultInjectedOnHttpMethod(HttpMethod.POST, "http://0.0.0.0:5004/post")) {
                expected = true;
                assertTrue(sre.getMessage().contains("Second RPC request to /world failed!"));

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key1", "value1");
                assertTrue(wasFaultInjectedOnHttpRequest(HttpMethod.POST, "http://0.0.0.0:.*/post", jsonObject.toString()));
            }

            assertTrue(wasFaultInjected(), "expected a fault to be injected");
            assertTrue(expected, "did not expect: " + sre);
        }
    }
}
