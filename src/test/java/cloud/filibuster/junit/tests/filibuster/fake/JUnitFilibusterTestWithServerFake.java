package cloud.filibuster.junit.tests.filibuster.fake;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.interceptors.FilibusterInvocationInterceptor;
import cloud.filibuster.junit.tests.filibuster.JUnitBaseTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.instrumentation.TestHelper.startMockFilibusterServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithServerFake extends JUnitBaseTest {
    private static int numberOfTestsExceptionsThrownFaultsInjected = 0;

    @BeforeAll
    protected static void disableFilibusterServerInitialization() throws IOException, InterruptedException {
        FilibusterInvocationInterceptor.shouldInitializeFilibusterServer = false;
        startMockFilibusterServerAndWaitUntilAvailable();
        FilibusterServer.shouldInjectGrpcMetadataFault = true;
    }

    @AfterAll
    protected static void enableFilibusterServerInitialization() throws InterruptedException {
        FilibusterInvocationInterceptor.shouldInitializeFilibusterServer = true;
        stopMockFilibusterServerAndWaitUntilUnavailable();
        FilibusterServer.shouldInjectGrpcMetadataFault = false;
    }

    /**
     * Inject faults between Hello and World using Filibuster and assert proper faults are injected.
     *
     * @throws InterruptedException if teardown of gRPC channel fails.
     */
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        boolean expected = false;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            boolean wasFaultInjected = wasFaultInjected();

            if (wasFaultInjected) {
                numberOfTestsExceptionsThrownFaultsInjected++;

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: NOT_FOUND")) {
                    expected = true;
                }

                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("world");
                assertTrue(wasFaultInjectedOnWorldService);

                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World");
                assertTrue(wasFaultInjectedOnWorldMethod);

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    /**
     * Verify that Filibuster generated the correct number of fault injections.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        // 3, because with the fake we inject the same fault three times
        assertEquals(3, numberOfTestsExceptionsThrownFaultsInjected);
    }
}