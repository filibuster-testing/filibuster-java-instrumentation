package cloud.filibuster.examples.armeria.grpc.tests.interceptors;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.armeria.grpc.test_services.MyHelloService;

import cloud.filibuster.instrumentation.FilibusterServer;

import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
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
public class HelloGrpcServerTestWithHelloAndWorldAndFilibusterServerTest extends HelloGrpcServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startHello();
        startWorld();
        startExternalServer();
        startFilibuster();
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
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
        FilibusterServer.resetPayloadsReceived();
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();
    }

    @BeforeEach
    public void disableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
        FilibusterServer.resetAdditionalExceptionMetadata();
    }

    @AfterEach
    public void enableFilibuster() {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;
    }

    @AfterEach
    public void resetFilibusterConfiguration() {
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
    }

    @Test
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws IOException, InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServer.grpcExceptionType = true;
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.additionalExceptionMetadata.put("code", "UNAVAILABLE");

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

        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.resetAdditionalExceptionMetadata();

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test partial hello server grpc route with Filibuster. (metadata fault, MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibusterMetadataFault() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServer.shouldInjectGrpcMetadataFault = true;

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

        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
        FilibusterServer.resetAdditionalExceptionMetadata();

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

        FilibusterServer.resetPayloadsReceived();
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.parallelPartialHello(request);
        assertEquals("Hello, Armerian World!! Hello, Parallel World!!", reply.getMessage());

        // Very proper number of Filibuster records.
        assertEquals(6, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-154-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/World-2ac0bec48de1a7dcae4633461bc34169923a7ae9-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-154-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/World-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        ArrayList<String> validDistributedExecutionIndexes = new ArrayList<>();
        validDistributedExecutionIndexes.add(firstRequestResolvedDistributedExecutionIndex.toString());
        validDistributedExecutionIndexes.add(secondRequestResolvedDistributedExecutionIndex.toString());

        // Scheduling nondeterminism means that we don't know what order the invocations, request_received, and
        // invocation_complete messages will arrive in.  However, we do know that each of them should exist
        // in a set of two distinct execution indexes and none should share a single EI (with a counter of 2.)
        //
        // Verify this.
        //
        for (int j = 0; j < 6; j++) {
            JSONObject payload = FilibusterServer.payloadsReceived.get(j);
            assertTrue(validDistributedExecutionIndexes.contains(payload.getString("execution_index")));
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
        FilibusterServer.resetAdditionalExceptionMetadata();

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
        assertEquals(3, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-61-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/World-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstRequestReceivedPayload.getString("execution_index"));

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
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
        assertEquals(5, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-238-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/WorldExternalHttp-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-238-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/WorldExternalHttp-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestResolvedDistributedExecutionIndex.push("world-MyWorldService.java-58-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());
        firstInvocationPayload = null;

        JSONObject firstInvocationRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstInvocationRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, firstInvocationRequestReceivedPayload.getInt("generated_id"));
        firstInvocationRequestReceivedPayload = null;

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals(secondRequestResolvedDistributedExecutionIndex.toString(), secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());
        secondInvocationPayload = null;

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestResolvedDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, secondInvocationCompletePayload.getInt("generated_id"));
        secondInvocationCompletePayload = null;

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
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
        assertEquals(6, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-279-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/WorldExternalGrpc-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestResolvedDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestResolvedDistributedExecutionIndex.push("hello-MyHelloService.java-279-cloud.filibuster.examples.WorldService-cloud.filibuster.examples.WorldService/WorldExternalGrpc-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestResolvedDistributedExecutionIndex.push("world-MyWorldService.java-94-cloud.filibuster.examples.HelloService-cloud.filibuster.examples.HelloService/Hello-0467af73e0837d51c48b70651c64b7e6537819d2-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());
        firstInvocationPayload = null;

        JSONObject firstInvocationRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstInvocationRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, firstInvocationRequestReceivedPayload.getInt("generated_id"));
        firstInvocationRequestReceivedPayload = null;

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals(secondRequestResolvedDistributedExecutionIndex.toString(), secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());
        secondInvocationPayload = null;

        JSONObject secondInvocationRequestReceivedPayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("request_received", secondInvocationRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(secondRequestResolvedDistributedExecutionIndex.toString(), secondInvocationRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, secondInvocationRequestReceivedPayload.getInt("generated_id"));
        secondInvocationRequestReceivedPayload = null;

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestResolvedDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, secondInvocationCompletePayload.getInt("generated_id"));
        secondInvocationCompletePayload = null;

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(5);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestResolvedDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
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