package cloud.filibuster.instrumentation.libraries.opentelemetry;

import cloud.filibuster.examples.test_servers.HelloServer;
import cloud.filibuster.examples.test_servers.WorldServer;
import cloud.filibuster.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OpenTelemetryHelloGrpcServerTestWithHelloAndWorldAndFilibusterServerPartialInstrumentationRaceDetectionTest extends OpenTelemetryHelloGrpcServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startHello();
        startWorld();
        startFilibuster();

        FilibusterServer.oneNewTestExecution = true;
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopFilibuster();
        stopWorld();
        stopHello();

        FilibusterServer.noNewTestExecution = false;
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
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
        FilibusterServer.resetPayloadsReceived();
        FilibusterServer.resetAdditionalExceptionMetadata();
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
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.grpcExceptionType = false;
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
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