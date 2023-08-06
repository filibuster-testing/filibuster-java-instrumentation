package cloud.filibuster.integration.examples.armeria.grpc.tests.interceptors;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;

import cloud.filibuster.integration.instrumentation.FilibusterServerFake;

import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;

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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelloGrpcServerTestWithHelloAndWorldAndFilibusterServerFakeTest extends HelloGrpcServerTest {
    @BeforeAll
    public static void startServices() throws IOException, InterruptedException {
        startHello();
        startWorld();
        startExternalServer();
        startFilibuster();
    }

    @AfterAll
    public static void stopServices() throws InterruptedException {
        stopFilibuster();
        stopExternalServer();
        stopWorld();
        stopHello();
    }

    @BeforeEach
    public void resetMyHelloServiceState() {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;
    }

    @BeforeEach
    public void resetFilibusterState() {
        FilibusterServerFake.resetPayloadsReceived();
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();
    }

    @BeforeEach
    public void disableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectGrpcMetadataFault = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();
    }

    @AfterEach
    public void enableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    @AfterEach
    public void resetFilibusterConfiguration() {
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectGrpcMetadataFault = false;
    }

    @Test
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws IOException, InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServerFake.grpcExceptionType = true;
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.additionalExceptionMetadata.put("code", "UNAVAILABLE");

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        });

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            blockingStub.partialHello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertEquals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE", re.getMessage());

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test partial hello server grpc route with Filibuster. (metadata fault, MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibusterMetadataFault() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServerFake.shouldInjectGrpcMetadataFault = true;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        assertThrows(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        });

        RuntimeException re;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            blockingStub.partialHello(request);
            throw new AssertionError("We shouldn't ever get here!");
        } catch (RuntimeException e) {
            re = e;
        }

        assertEquals("DATA_LOSS: io.grpc.StatusRuntimeException: NOT_FOUND", re.getMessage());

        assertTrue(wasFaultInjected());
        assertTrue(wasFaultInjectedOnService("world"));

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.shouldInjectGrpcMetadataFault = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test parallel partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibusterParallelNoFault() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        FilibusterServerFake.resetPayloadsReceived();
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.parallelPartialHello(request);
        assertEquals("Hello, Armerian World!! Hello, Parallel World!!", reply.getMessage());

        // Very proper number of Filibuster records.
        assertEquals(6, FilibusterServerFake.payloadsReceived.size());

        ArrayList<String> validDistributedExecutionIndexes = new ArrayList<>();
        validDistributedExecutionIndexes.add("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-6550c6956ce85982ca3b4a1bfb4341d3a3b4cd0d-32695a1108625346a7ea2158439d36cd0396aee3\", 1]]");
        validDistributedExecutionIndexes.add("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-6550c6956ce85982ca3b4a1bfb4341d3a3b4cd0d-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]");

        // Scheduling nondeterminism means that we don't know what order the invocations, request_received, and
        // invocation_complete messages will arrive in.  However, we do know that each of them should exist
        // in a set of two distinct execution indexes and none should share a single EI (with a counter of 2.)
        //
        // Verify this.
        //
        for (int j = 0; j < 6; j++) {
            JSONObject payload = FilibusterServerFake.payloadsReceived.get(j);
            assertTrue(validDistributedExecutionIndexes.contains(payload.getString("execution_index")));
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.shouldInjectGrpcMetadataFault = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test partial hello server grpc route. (with rendezvous, MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceVerifyRendezvous() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHello(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

        // Very proper number of Filibuster records.
        assertEquals(3, FilibusterServerFake.payloadsReceived.size());

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-bf2e37f662587294c6c6468358da16c232f42fcc-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-bf2e37f662587294c6c6468358da16c232f42fcc-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-bf2e37f662587294c6c6468358da16c232f42fcc-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test partial external hello http server grpc route. (with rendezvous, MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceExternalHttpRoute() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHelloExternalHttp(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

        // Very proper number of Filibuster records.
        assertEquals(5, FilibusterServerFake.payloadsReceived.size());

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-07b1a0d6b1748a4e44384cce19bb13bb84b70289-c5ae426c0e338b03f750b8963fd3010cee50be0a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());
        firstInvocationPayload = null;

        JSONObject firstInvocationRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstInvocationRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-07b1a0d6b1748a4e44384cce19bb13bb84b70289-c5ae426c0e338b03f750b8963fd3010cee50be0a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, firstInvocationRequestReceivedPayload.getInt("generated_id"));
        firstInvocationRequestReceivedPayload = null;

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-07b1a0d6b1748a4e44384cce19bb13bb84b70289-c5ae426c0e338b03f750b8963fd3010cee50be0a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-97094b6a010685cf94ff6050f3e7d47808fad43b-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());
        secondInvocationPayload = null;

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-07b1a0d6b1748a4e44384cce19bb13bb84b70289-c5ae426c0e338b03f750b8963fd3010cee50be0a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-97094b6a010685cf94ff6050f3e7d47808fad43b-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, secondInvocationCompletePayload.getInt("generated_id"));
        secondInvocationCompletePayload = null;

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-07b1a0d6b1748a4e44384cce19bb13bb84b70289-c5ae426c0e338b03f750b8963fd3010cee50be0a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, firstInvocationCompletePayload.getInt("generated_id"));
        firstInvocationCompletePayload = null;

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test partial external hello grpc server grpc route. (with rendezvous, MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceExternalGrpcRoute() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHelloExternalGrpc(request);
        assertEquals("Hello, Hello, Hello, Armerian!!", reply.getMessage());

        // Very proper number of Filibuster records.
        assertEquals(6, FilibusterServerFake.payloadsReceived.size());

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-623ea48b2fcd64aa41a188f486cbce10d11817db-4e2178d1d6a088d306ed40e1c773a618a767d36a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());
        firstInvocationPayload = null;

        JSONObject firstInvocationRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstInvocationRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-623ea48b2fcd64aa41a188f486cbce10d11817db-4e2178d1d6a088d306ed40e1c773a618a767d36a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, firstInvocationRequestReceivedPayload.getInt("generated_id"));
        firstInvocationRequestReceivedPayload = null;

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-623ea48b2fcd64aa41a188f486cbce10d11817db-4e2178d1d6a088d306ed40e1c773a618a767d36a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-02be70093aa1244da10bd3b32514e8b3233ac30e-293f9d04b223505c718930ae5136455de9476400-1457c6f32cd01e387cfce1b8e17cdf8ac54887bf\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());
        secondInvocationPayload = null;

        JSONObject secondInvocationRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondInvocationRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-623ea48b2fcd64aa41a188f486cbce10d11817db-4e2178d1d6a088d306ed40e1c773a618a767d36a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-02be70093aa1244da10bd3b32514e8b3233ac30e-293f9d04b223505c718930ae5136455de9476400-1457c6f32cd01e387cfce1b8e17cdf8ac54887bf\", 1]]", secondInvocationRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, secondInvocationRequestReceivedPayload.getInt("generated_id"));
        secondInvocationRequestReceivedPayload = null;

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-623ea48b2fcd64aa41a188f486cbce10d11817db-4e2178d1d6a088d306ed40e1c773a618a767d36a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-02be70093aa1244da10bd3b32514e8b3233ac30e-293f9d04b223505c718930ae5136455de9476400-1457c6f32cd01e387cfce1b8e17cdf8ac54887bf\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, secondInvocationCompletePayload.getInt("generated_id"));
        secondInvocationCompletePayload = null;

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-623ea48b2fcd64aa41a188f486cbce10d11817db-4e2178d1d6a088d306ed40e1c773a618a767d36a-3c1cee3739e0ed5d0c169309c4a17650cd7fb4a4\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, firstInvocationCompletePayload.getInt("generated_id"));
        firstInvocationCompletePayload = null;

        helloChannel.shutdownNow();
        while (! helloChannel.awaitTermination(1000, TimeUnit.SECONDS)) {
            Thread.sleep(4000);
        }

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }
}