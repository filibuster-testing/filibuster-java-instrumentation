package cloud.filibuster.integration.examples.armeria.grpc.tests.decorators.scenarios;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.integration.examples.armeria.grpc.tests.decorators.HelloGrpcServerTest;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.integration.examples.test_servers.HelloServer;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"ResultOfMethodCallIgnored", "DeduplicateConstants"})
public class HelloGrpcServerTestWithHelloAndFilibusterServerFakeWithDescriptionTest extends HelloGrpcServerTest {
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
    public void resetContextConfiguration() {
        HelloServer.resetInitialDistributedExecutionIndex();
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
        HelloServer.setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = true;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;

        MyHelloService.shouldReturnRuntimeExceptionWithDescription = true;
    }

    @AfterEach
    public void teardownFalseAbortScenario() {
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available and exception with description.")
    public void testMyHelloServiceWithFilibusterAndExceptionWithDescription() throws InterruptedException {
        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                    .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        waitForWaitComplete();

        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("FAILED_PRECONDITION", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("code"));
        assertEquals("io.grpc.StatusRuntimeException", lastPayload.getJSONObject("exception").getString("name"));

        VectorClock assertVc = generateAssertionClock();
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-1aa66c5ca169c200037d10fbf936196e58acbdba-e3b858152435d0cadc132349ad51d97a785b20b4\", 1]]", lastPayload.getString("execution_index"));
    }
}