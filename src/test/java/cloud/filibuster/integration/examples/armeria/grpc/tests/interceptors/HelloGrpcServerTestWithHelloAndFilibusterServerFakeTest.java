package cloud.filibuster.integration.examples.armeria.grpc.tests.interceptors;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;

import cloud.filibuster.integration.examples.test_servers.HelloServer;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelloGrpcServerTestWithHelloAndFilibusterServerFakeTest extends HelloGrpcServerTest {
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
        HelloServer.resetInitialDistributedExecutionIndex();
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
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldNotAbort = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
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
        HelloServer.setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
        Hello.HelloReply reply = blockingStub.hello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("test");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-d8f0994dce26f9a4843957b89c4192f90c0a3f18-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", lastPayload.getString("execution_index"));

        assertFalse(wasFaultInjected());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available and exception with description.")
    public void testMyHelloServiceWithFilibusterAndExceptionWithDescription() {
        HelloServer.setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("FAILED_PRECONDITION", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("code"));
        assertEquals("io.grpc.StatusRuntimeException", lastPayload.getJSONObject("exception").getString("name"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("test");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-2addd3a570d4f81781e428ce50ae5e2351ea0b53-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", lastPayload.getString("execution_index"));

        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster server available and exception with cause.")
    public void testMyHelloServiceWithFilibusterAndExceptionWithCause() {
        HelloServer.setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
        MyHelloService.shouldReturnRuntimeExceptionWithCause = true;

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(channel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian World").build();
            blockingStub.hello(request);
        });

        assertEquals(3, FilibusterServerFake.payloadsReceived.size());

        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("FAILED_PRECONDITION", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("code"));
        assertEquals("io.grpc.StatusRuntimeException", lastPayload.getJSONObject("exception").getString("name"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("test");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-3c4a3a7e331e0ea93551da80626b945693d27038-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", lastPayload.getString("execution_index"));

        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster and fault injection.")
    public void testMyHelloServiceWithFilibusterWithFaultInjection() {
        HelloServer.setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServerFake.grpcExceptionType = true;
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.additionalExceptionMetadata.put("code", "FAILED_PRECONDITION");

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

        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-375b79efce084fd03741cae89fc3ecee53cc959e-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-375b79efce084fd03741cae89fc3ecee53cc959e-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;

        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
    }

    @Test
    @DisplayName("Test hello server grpc route with Filibuster and fault injection (with false abort.)")
    public void testMyHelloServiceWithFilibusterWithFaultInjectionWithFalseAbort() {
        HelloServer.setupLocalFixtures();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServerFake.shouldNotAbort = true;
        FilibusterServerFake.grpcExceptionType = true;
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.additionalExceptionMetadata.put("code", "FAILED_PRECONDITION");

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

        assertEquals(3, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-5a3e8bd7aeb7c4facf36afc5182ae762131c6a93-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-5a3e8bd7aeb7c4facf36afc5182ae762131c6a93-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-5a3e8bd7aeb7c4facf36afc5182ae762131c6a93-7321335838ea883cd005a01a7e721e2d6970fe2f\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;

        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
    }
}