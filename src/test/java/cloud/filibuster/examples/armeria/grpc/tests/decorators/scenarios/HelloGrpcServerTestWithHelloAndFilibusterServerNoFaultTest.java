package cloud.filibuster.examples.armeria.grpc.tests.decorators.scenarios;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.armeria.grpc.tests.decorators.HelloGrpcServerTest;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cloud.filibuster.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.examples.test_servers.HelloServer.setupLocalFixtures;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test end-to-end functionality when Filibuster is available but no faults are injected.
 */
@SuppressWarnings({"DeduplicateConstants"})
public class HelloGrpcServerTestWithHelloAndFilibusterServerNoFaultTest extends HelloGrpcServerTest {
    private GrpcClientBuilder grpcClientBuilder;
    private static final String serviceName = "test";

    @BeforeEach
    protected void startServices() throws IOException, InterruptedException {
        startHello();
        startFilibuster();
    }

    @AfterEach
    protected void stopServices() throws InterruptedException {
        stopFilibuster();
        stopHello();
    }

    @BeforeEach
    protected void resetContextConfiguration() {
        resetInitialDistributedExecutionIndex();
    }

    @BeforeEach
    protected void setupGrpcClientBuilder() {
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
    }

    @BeforeEach
    protected void setupFalseAbortScenario() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = true;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
    }

    private static VectorClock generateAssertionClock() {
        VectorClock vectorClock = new VectorClock();
        vectorClock.incrementClock("test");
        return vectorClock;
    }

    @AfterEach
    protected void teardownFalseAbortScenario() {
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    /**
     * Test end-to-end functionality when Filibuster is available but no faults are injected.
     */
    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available.")
    public void testMyHelloServiceWithFilibuster() throws InterruptedException {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
        Hello.HelloReply reply = blockingStub.hello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

        waitForWaitComplete();

        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = generateAssertionClock();
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("test-HelloGrpcServerTestWithHelloAndFilibusterServerNoFaultTest.java-89-cloud.filibuster.examples.HelloService-cloud.filibuster.examples.HelloService/Hello-146409d9c7d501362ce2f58ab555782fba01c7c6-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        assertFalse(wasFaultInjected());
    }
}