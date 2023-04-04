package cloud.filibuster.integration.instrumentation.libraries.opentelemetry;

import cloud.filibuster.integration.examples.test_servers.HelloServer;
import cloud.filibuster.integration.examples.test_servers.WorldServer;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OpenTelemetryHelloGrpcServerTestWithHelloAndWorldAndFilibusterServerFakePartialInstrumentationRaceDetectionTest extends OpenTelemetryHelloGrpcServerTest {
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
    public void resetConfigurationBeforeAll() {
        FilibusterServerFake.oneNewTestExecution = true;
    }

    @AfterEach
    public void resetConfigurationAfterAll() {
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
        HelloServer.useOtelServerInterceptor = true;
        WorldServer.useOtelServerInterceptor = false;
    }

    @AfterEach
    public void disableOtelInstrumentor() {
        MyHelloService.useOtelClientInterceptor = false;
        HelloServer.useOtelServerInterceptor = false;
        WorldServer.useOtelServerInterceptor = false;
    }

//    @Test
//    @DisplayName("Test partial hello server grpc route with Filibuster. (exception fault, MyHelloService, MyWorldService)")
//    public void testMyHelloAndMyWorldServiceWithFilibusterExceptionFault() throws InterruptedException {
//        FilibusterClientInterceptor.disableInstrumentation = false;
//        FilibusterServerInterceptor.disableInstrumentation = false;
//
//        FilibusterServer.grpcExceptionType = true;
//        FilibusterServer.shouldInjectExceptionFault = true;
//        FilibusterServer.additionalExceptionMetadata.put("code", "UNAVAILABLE");
//
//        ManagedChannel helloChannel = ManagedChannelBuilder
//                .forAddress(Helper.getHost("hello"), Helper.getPort("hello"))
//                .usePlaintext()
//                .build();
//
//        RuntimeException re;
//
//         for (int i = 0; i < 20; i++) {
//             FilibusterServer.resetPayloadsReceived();
//             FilibusterClientInstrumentor.clearVectorClockForRequestId();
//             FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
//             re = null;
//
//            try {
//                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
//                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
//                blockingStub.partialHello(request);
//                throw new AssertionError("We shouldn't ever get here!");
//            } catch (RuntimeException e) {
//                re = e;
//            }
//
//            assertEquals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE", re.getMessage());
//
//            assertTrue(wasFaultInjected());
//            assertTrue(wasFaultInjectedOn("world"));
//
//            assertEquals(2, FilibusterServer.payloadsReceived.size());
//        }
//
//        helloChannel.shutdownNow();
//        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
//
//        FilibusterServer.grpcExceptionType = false;
//        FilibusterServer.shouldInjectExceptionFault = false;
//        FilibusterServer.resetAdditionalExceptionMetadata();
//
//        FilibusterClientInterceptor.disableInstrumentation = true;
//        FilibusterServerInterceptor.disableInstrumentation = true;
//    }
//
//    @Test
//    @DisplayName("Test partial hello server grpc route with Filibuster. (metadata fault, MyHelloService, MyWorldService)")
//    public void testMyHelloAndMyWorldServiceWithFilibusterMetadataFault() throws InterruptedException {
//        FilibusterClientInterceptor.disableInstrumentation = false;
//        FilibusterServerInterceptor.disableInstrumentation = false;
//
//        FilibusterServer.shouldInjectGrpcMetadataFault = true;
//
//        ManagedChannel helloChannel = ManagedChannelBuilder
//                .forAddress(Helper.getHost("hello"), Helper.getPort("hello"))
//                .usePlaintext()
//                .build();
//
//        RuntimeException re;
//
//        for (int i = 0; i < 1000; i++) {
//            FilibusterServer.resetPayloadsReceived();
//            FilibusterClientInstrumentor.clearVectorClockForRequestId();
//            FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
//            re = null;
//
//            try {
//                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
//                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
//                blockingStub.partialHello(request);
//                throw new AssertionError("We shouldn't ever get here!");
//            } catch (RuntimeException e) {
//                re = e;
//            }
//
//            assertEquals("DATA_LOSS: io.grpc.StatusRuntimeException: NOT_FOUND", re.getMessage());
//
//            assertTrue(wasFaultInjected());
//            assertTrue(wasFaultInjectedOn("world"));
//
//            assertEquals(2, FilibusterServer.payloadsReceived.size());
//        }
//
//        helloChannel.shutdownNow();
//        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
//
//        FilibusterServer.grpcExceptionType = false;
//        FilibusterServer.shouldInjectExceptionFault = false;
//        FilibusterServer.shouldInjectGrpcMetadataFault = false;
//        FilibusterServer.resetAdditionalExceptionMetadata();
//
//        FilibusterClientInterceptor.disableInstrumentation = true;
//        FilibusterServerInterceptor.disableInstrumentation = true;
//    }
}