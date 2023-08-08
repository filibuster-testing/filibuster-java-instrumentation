package cloud.filibuster.integration.examples.armeria.grpc.tests.decorators.scenarios;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.tests.decorators.HelloGrpcServerTest;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.integration.examples.test_servers.HelloServer;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test end-to-end functionality when Filibuster is available but no faults are injected.
 */
@SuppressWarnings({"DeduplicateConstants"})
public class HelloGrpcServerTestWithHelloAndFilibusterServerFakeNoFaultTest extends HelloGrpcServerTest {
    private GrpcClientBuilder grpcClientBuilder;
    private static final String serviceName = "test";

    @BeforeAll
    public static void startServices() throws IOException, InterruptedException {
        startHello();
        startFilibuster();
    }

    @AfterAll
    public static void stopServices() throws InterruptedException {
        stopFilibuster();
        stopHello();
    }

    @BeforeEach
    protected void resetContextConfiguration() {
        HelloServer.resetInitialDistributedExecutionIndex();
    }

    @BeforeEach
    protected void setupGrpcClientBuilder() {
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
    }

    @BeforeEach
    protected void setupFalseAbortScenario() {
        HelloServer.setupLocalFixtures();

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
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @DisplayName("Test hello server grpc route with Filibuster server available.")
    public void testMyHelloServiceWithFilibuster() throws InterruptedException {
        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
        Hello.HelloReply reply = blockingStub.hello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

        waitForWaitComplete();

        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = generateAssertionClock();
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-f39995b2a13f48a1157e6db81088379d6b034a8a-e3b858152435d0cadc132349ad51d97a785b20b4\", 1]]", lastPayload.getString("execution_index"));

        assertFalse(wasFaultInjected());
    }
}