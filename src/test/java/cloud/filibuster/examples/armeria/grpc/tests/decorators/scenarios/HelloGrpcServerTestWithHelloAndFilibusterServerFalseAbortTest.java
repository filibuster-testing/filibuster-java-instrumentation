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
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static cloud.filibuster.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.examples.test_servers.HelloServer.setupLocalFixtures;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"ResultOfMethodCallIgnored", "DeduplicateConstants"})
public class HelloGrpcServerTestWithHelloAndFilibusterServerFalseAbortTest extends HelloGrpcServerTest {
    private GrpcClientBuilder grpcClientBuilder;
    private static final String serviceName = "test";

    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startHello();
        startFilibuster();
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopFilibuster();
        stopHello();
    }

    @BeforeEach
    public void resetContextConfiguration() {
        resetInitialDistributedExecutionIndex();
    }

    @BeforeEach
    public void setupGrpcClientBuilder() {
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, serviceName);
    }

    public static VectorClock generateAssertionClock() {
        VectorClock vectorClock = new VectorClock();
        vectorClock.incrementClock("test");
        return vectorClock;
    }

    @BeforeEach
    public void setupFalseAbortScenario() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;

        FilibusterServer.shouldNotAbort = true;
        FilibusterServer.grpcExceptionType = true;
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.additionalExceptionMetadata.put("code", "FAILED_PRECONDITION");
    }

    @AfterEach
    public void teardownFalseAbortScenario() {
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldNotAbort = false;
        FilibusterServer.resetAdditionalExceptionMetadata();
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster and fault injection (with false abort.)")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    public void testMyHelloServiceWithFilibusterWithFaultInjectionWithFalseAbort() {
        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                    .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertEquals("FAILED_PRECONDITION", re.getMessage());

        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-5d06ab97c95040ab64772a06f37d5216274fd55f-146409d9c7d501362ce2f58ab555782fba01c7c6");

        VectorClock firstRequestVectorClock = generateAssertionClock();

        assertEquals(3, FilibusterServer.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstRequestReceivedPayload.getString("execution_index"));

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
    }
}