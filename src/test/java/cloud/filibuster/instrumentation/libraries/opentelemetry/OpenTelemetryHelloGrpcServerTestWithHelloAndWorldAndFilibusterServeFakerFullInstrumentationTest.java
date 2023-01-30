package cloud.filibuster.instrumentation.libraries.opentelemetry;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.test_servers.HelloServer;
import cloud.filibuster.integration.examples.test_servers.WorldServer;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyWorldService;
import cloud.filibuster.instrumentation.FilibusterServerFake;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OpenTelemetryHelloGrpcServerTestWithHelloAndWorldAndFilibusterServeFakerFullInstrumentationTest extends OpenTelemetryHelloGrpcServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startHello();
        startWorld();
        startExternalServer();
        startFilibuster();

        FilibusterServerFake.oneNewTestExecution = true;
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopFilibuster();
        stopExternalServer();
        stopWorld();
        stopHello();

        FilibusterServerFake.noNewTestExecution = false;
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
        FilibusterServerFake.shouldInjectGrpcMetadataFault = false;
        FilibusterServerFake.resetPayloadsReceived();
        FilibusterServerFake.resetAdditionalExceptionMetadata();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
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

    @BeforeEach
    public void enableOtelInstrumentor() {
        MyHelloService.useOtelClientInterceptor = true;
        MyWorldService.useOtelClientInterceptor = true;
        HelloServer.useOtelServerInterceptor = true;
        WorldServer.useOtelServerInterceptor = true;
    }

    @AfterEach
    public void disableOtelInstrumentor() {
        MyHelloService.useOtelClientInterceptor = false;
        MyWorldService.useOtelClientInterceptor = false;
        HelloServer.useOtelServerInterceptor = false;
        WorldServer.useOtelServerInterceptor = false;
    }

    @Test
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
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

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstInvocationPayload.getString("execution_index"));
        firstInvocationPayload = null;

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstRequestReceivedPayload.getString("execution_index"));
        assertEquals(0, firstRequestReceivedPayload.getInt("generated_id"));
        firstRequestReceivedPayload = null;

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
        assertEquals(0, firstInvocationCompletePayload.getInt("generated_id"));
        firstInvocationCompletePayload = null;

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }

    @Test
    @DisplayName("Test partial hello server grpc route with Filibuster. (exception fault, MyHelloService, MyWorldService)")
    public void testMyHelloAndMyWorldServiceWithFilibusterExceptionFault() throws InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServerFake.grpcExceptionType = true;
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.additionalExceptionMetadata.put("code", "UNAVAILABLE");

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

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

        // Assemble vector clocks.
        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

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
    public void testMyHelloAndMyWorldServiceWithFilibusterMetadataFault() throws IOException, InterruptedException {
        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

        FilibusterServerFake.shouldInjectGrpcMetadataFault = true;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

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

        // Assemble vector clocks.
        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-308d1419e1ba0da4af15c810881dec2f4c11dba9-474e350ca19ed3b62165ba0d6fc7de4dc2bd2418-0467af73e0837d51c48b70651c64b7e6537819d2\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        FilibusterServerFake.grpcExceptionType = false;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.shouldInjectGrpcMetadataFault = false;
        FilibusterServerFake.resetAdditionalExceptionMetadata();

        FilibusterClientInterceptor.disableInstrumentation = true;
        FilibusterServerInterceptor.disableInstrumentation = true;
    }
}