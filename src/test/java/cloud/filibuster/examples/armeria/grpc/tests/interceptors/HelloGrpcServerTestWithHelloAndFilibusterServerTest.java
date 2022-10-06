package cloud.filibuster.examples.armeria.grpc.tests.interceptors;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.examples.test_servers.HelloServer.setupLocalFixtures;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelloGrpcServerTestWithHelloAndFilibusterServerTest extends HelloGrpcServerTest {
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
    public void resetMyHelloServiceState() {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;
    }

    @BeforeEach
    public void disableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldNotAbort = false;
        FilibusterServer.resetAdditionalExceptionMetadata();
    }

    @AfterEach
    public void enableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    private Channel channel;
    private ManagedChannel originalChannel;

    @BeforeEach
    public void setupChannel() {
        originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("test");
        channel = ClientInterceptors.intercept(originalChannel, clientInterceptor);
    }

    @AfterEach
    public void teardownChannel() throws InterruptedException {
        originalChannel.shutdownNow();
        originalChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available.")
    public void testMyHelloServiceWithFilibuster() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
        Hello.HelloReply reply = blockingStub.hello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("test");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-cac2a2eab53c0eb00b365eee92dfc400c1d5995f-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", lastPayload.getString("execution_index"));

        assertFalse(wasFaultInjected());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available and exception with description.")
    public void testMyHelloServiceWithFilibusterAndExceptionWithDescription() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("FAILED_PRECONDITION", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("code"));
        assertEquals("io.grpc.StatusRuntimeException", lastPayload.getJSONObject("exception").getString("name"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("test");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-0d639a1f4fd0952fce7a93ffc371b2d95c4817f1-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", lastPayload.getString("execution_index"));

        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available and exception with cause.")
    public void testMyHelloServiceWithFilibusterAndExceptionWithCause() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
        MyHelloService.shouldReturnRuntimeExceptionWithCause = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        assertEquals(3, FilibusterServer.payloadsReceived.size());

        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("FAILED_PRECONDITION", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("code"));
        assertEquals("io.grpc.StatusRuntimeException", lastPayload.getJSONObject("exception").getString("name"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("test");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-4260cf8c1665fdf3445c0057f3d3ed6feb0e9f46-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", lastPayload.getString("execution_index"));

        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster and fault injection.")
    public void testMyHelloServiceWithFilibusterWithFaultInjection() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServer.grpcExceptionType = true;
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.additionalExceptionMetadata.put("code", "FAILED_PRECONDITION");

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertEquals("FAILED_PRECONDITION", re.getMessage());

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("test");

        assertEquals(2, FilibusterServer.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-174adee3b8b7333d81da6915880dfc80aadbca24-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-174adee3b8b7333d81da6915880dfc80aadbca24-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;

        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.resetAdditionalExceptionMetadata();
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster and fault injection (with false abort.)")
    public void testMyHelloServiceWithFilibusterWithFaultInjectionWithFalseAbort() {
        setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServer.shouldNotAbort = true;
        FilibusterServer.grpcExceptionType = true;
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.additionalExceptionMetadata.put("code", "FAILED_PRECONDITION");

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertEquals("FAILED_PRECONDITION", re.getMessage());

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("test");

        assertEquals(3, FilibusterServer.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-13bd8ae0013ed89f0746058b5a93091bb35d8bd4-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-13bd8ae0013ed89f0746058b5a93091bb35d8bd4-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-13bd8ae0013ed89f0746058b5a93091bb35d8bd4-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;

        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.resetAdditionalExceptionMetadata();
    }
}