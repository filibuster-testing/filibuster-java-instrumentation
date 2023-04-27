package cloud.filibuster.functional.java.bfs;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFilibuster;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterHelloPartialHelloWithErrorHandlingTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestsExecuted = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(searchStrategy= FilibusterSearchStrategy.BFS, maxIterations=10)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        numberOfTestsExecuted++;

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.partialHelloWithErrorHandling(request);
        assertEquals("Hello, Armerian World!!", reply.getMessage());

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
        assertEquals(6, numberOfTestsExecuted);
    }
}