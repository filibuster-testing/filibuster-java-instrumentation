package cloud.filibuster.functional.java.smoke.extended;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.server.backends.FilibusterLocalServerBackend;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify that fault injection works with Filibuster when no beforeEach or afterEach is present.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithNoBeforeEachOrAfterEach {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    /**
     * Verify that fault injection works with Filibuster when no beforeEach or afterEach is present.
     *
     * @throws InterruptedException thrown when channel teardown or server fails to initialize or shutdown.
     * @throws IOException thrown when trying to start up dependent servers.
     */
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(serverBackend=FilibusterLocalServerBackend.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException, IOException {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;

        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
        startExternalServerAndWaitUntilAvailable();

        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();

        FilibusterClientInterceptor.disableInstrumentation = false;
        FilibusterServerInterceptor.disableInstrumentation = false;

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
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                testExceptionsThrown.add(t.getMessage());

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INVALID_ARGUMENT")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                }

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);

        stopHelloServerAndWaitUntilUnavailable();
        stopWorldServerAndWaitUntilUnavailable();
        stopExternalServerAndWaitUntilUnavailable();
    }

    /**
     * Verify that Filibuster generates the correct number of tests.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEqualsUnlessFilibusterDisabledByEnvironment(4, testExceptionsThrown.size());
    }

    private static void assertEqualsUnlessFilibusterDisabledByEnvironment(int expected, int actual) {
        if (System.getenv("FILIBUSTER_DISABLED") != null) {
            Assert.assertEquals(0, actual);
        } else {
            Assert.assertEquals(expected, actual);
        }
    }
}