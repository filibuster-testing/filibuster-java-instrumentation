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
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cloud.filibuster.integration.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.integration.examples.test_servers.HelloServer.setupLocalFixtures;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"ResultOfMethodCallIgnored", "DeduplicateConstants"})
public class HelloGrpcServerTestWithHelloAndFilibusterServerFakeFaultTest extends HelloGrpcServerTest {
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

        FilibusterServerFake.grpcExceptionType = true;
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.additionalExceptionMetadata.put("code", "FAILED_PRECONDITION");
    }

    @AfterEach
    public void teardownFalseAbortScenario() {
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster and fault injection.")
    public void testMyHelloServiceWithFilibusterWithFaultInjection() {
        StatusRuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                    .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (StatusRuntimeException e) {
            re = e;
        }

        assertEquals("FAILED_PRECONDITION", re.getStatus().getCode().name());

        VectorClock firstRequestVectorClock = generateAssertionClock();

        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-4785660e27df4252b84e45fa14c98d2cead81284-e3b858152435d0cadc132349ad51d97a785b20b4\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-4785660e27df4252b84e45fa14c98d2cead81284-e3b858152435d0cadc132349ad51d97a785b20b4\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
    }
}