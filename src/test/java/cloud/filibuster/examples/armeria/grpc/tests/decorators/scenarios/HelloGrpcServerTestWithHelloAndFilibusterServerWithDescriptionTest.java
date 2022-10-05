package cloud.filibuster.examples.armeria.grpc.tests.decorators.scenarios;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.examples.armeria.grpc.tests.decorators.HelloGrpcServerTest;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
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

import static cloud.filibuster.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.examples.test_servers.HelloServer.setupLocalFixtures;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"ResultOfMethodCallIgnored", "DeduplicateConstants"})
public class HelloGrpcServerTestWithHelloAndFilibusterServerWithDescriptionTest extends HelloGrpcServerTest {
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

        assertEquals(2, FilibusterServer.payloadsReceived.size());

        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("FAILED_PRECONDITION", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("code"));
        assertEquals("io.grpc.StatusRuntimeException", lastPayload.getJSONObject("exception").getString("name"));

        VectorClock assertVc = generateAssertionClock();
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("test-HelloGrpcServerTestWithHelloAndFilibusterServerWithDescriptionTest.java-84-cloud.filibuster.examples.HelloService-cloud.filibuster.examples.HelloService/Hello-146409d9c7d501362ce2f58ab555782fba01c7c6-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));
    }
}