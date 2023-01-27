package cloud.filibuster.junit.tests.filibuster.smoke.local.hello;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.server.backends.FilibusterLocalServerBackend;
import cloud.filibuster.junit.tests.filibuster.JUnitBaseTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethodWherePayloadContains;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnRequest;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterHelloHelloTest extends JUnitBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestsExecuted = 0;

    private static int numberOfExceptionsThrown = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(serverBackend=FilibusterLocalServerBackend.class, maxIterations=10)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        numberOfTestsExecuted++;

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.hello(request);
        assertEquals("Hello, Armerian!!", reply.getMessage());
        assertFalse(wasFaultInjected());

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of thrown exceptions.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(0, testExceptionsThrown.size());
    }

    @DisplayName("Verify correct number of executed tests.")
    @Test
    @Order(3)
    public void testNumberOfTestsExecuted() {
        assertEquals(1, numberOfTestsExecuted);
    }

    @DisplayName("Verify correct number of exceptions thrown.")
    @Test
    @Order(4)
    public void numberOfExceptionsThrown() {
        assertEquals(0, numberOfExceptionsThrown);
    }
}