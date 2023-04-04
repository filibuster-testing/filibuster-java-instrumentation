package cloud.filibuster.functional.java.hello.multiple;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.JUnitBaseTest;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.server.backends.FilibusterLocalServerBackend;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterHelloParallelSynchronousPartialHelloTest extends JUnitAnnotationBaseTest {
    private final static Set<String> responsesReceived = new HashSet<>();

    private final List possibleResponses = Arrays.asList(
            "Hello, Armerian World!! Hello, Parallel World!!",
            "Hello, Parallel World!!",
            "Hello, Armerian World!!",
            "Hello, "
    );

    private static int numberOfTestsExecuted = 0;
    
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(maxIterations=100)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        numberOfTestsExecuted++;

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();

        Hello.HelloReply reply = blockingStub.parallelSynchronousPartialHello(request);
        assertEquals(true, possibleResponses.contains(reply.getMessage()));
        responsesReceived.add(reply.getMessage());

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of possible responses.")
    @Test
    @Order(2)
    public void testResponsesReceived() {
        assertEquals(possibleResponses.size(), responsesReceived.size());
    }

    @DisplayName("Verify correct number of executed tests.")
    @Test
    @Order(3)
    public void testNumberOfTestsExecuted() {
        assertEquals(36, numberOfTestsExecuted);
    }
}