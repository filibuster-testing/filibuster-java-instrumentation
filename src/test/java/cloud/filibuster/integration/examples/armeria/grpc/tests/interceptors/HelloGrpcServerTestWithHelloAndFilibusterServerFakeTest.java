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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelloGrpcServerTestWithHelloAndFilibusterServerFakeTest extends HelloGrpcServerTest {
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
    public void resetPayloadsReceived() {
        FilibusterServerFake.resetPayloadsReceived();
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

        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-4e89b6897edc3c6dc30cc7418323eab30330c24c-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", lastPayload.getString("execution_index"));

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

        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-7e8c44021a00d4cd9d5e8a10b411af4752ed2d08-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", lastPayload.getString("execution_index"));

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

        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-ee094f0de4e3e456c494cff85458fa7a04a6478d-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", lastPayload.getString("execution_index"));

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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-07a3625eb55e434fba553f90e97b2e23ddbe8479-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJsonObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-07a3625eb55e434fba553f90e97b2e23ddbe8479-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJsonObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-da040267a2cb07a1bd9a295ec9600a8fc98fa2ac-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJsonObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-da040267a2cb07a1bd9a295ec9600a8fc98fa2ac-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-a94a8fe5ccb19ba61c4c0873d391e987982fbbd3-02be70093aa1244da10bd3b32514e8b3233ac30e-da040267a2cb07a1bd9a295ec9600a8fc98fa2ac-51d0c6d179f06a73c7c08e98dc587a0f89598884\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJsonObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;

        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
    }
}