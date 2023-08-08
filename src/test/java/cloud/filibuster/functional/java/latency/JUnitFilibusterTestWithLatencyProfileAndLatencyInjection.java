package cloud.filibuster.functional.java.latency;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.exceptions.filibuster.FilibusterAllowedTimeExceededException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.FilibusterLatencyOnlyAnalysisConfigurationFile;
import cloud.filibuster.junit.server.latency.Filibuster1000msLatencyProfile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithLatencyProfileAndLatencyInjection extends JUnitAnnotationBaseTest {
    private static int numberOfTestsExecuted = 0;

    private static int numberOfExceptions = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(analysisConfigurationFile=FilibusterLatencyOnlyAnalysisConfigurationFile.class, latencyProfile=Filibuster1000msLatencyProfile.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        numberOfTestsExecuted++;

        try {
            assertPassesWithinMs(2000, () -> {
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);
                assertEquals("Hello, Armerian World!!", reply.getMessage());
            });
        } catch (FilibusterAllowedTimeExceededException e) {
            numberOfExceptions++;
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @Test
    @Order(2)
    public void testNumberOfTestsExecuted() {
        assertEquals(2, numberOfTestsExecuted);
    }

    @Test
    @Order(2)
    public void testNumberOfExceptions() {
        assertEquals(1, numberOfExceptions);
    }

    private static void assertPassesWithinMs(int milliseconds, Runnable testBlock) {
        long startTime = System.nanoTime();
        testBlock.run();
        long endTime = System.nanoTime();

        long duration = (endTime - startTime);
        long durationMs = duration / 1000000;
        if (durationMs > milliseconds) {
            throw new FilibusterAllowedTimeExceededException("Test completed in " + durationMs +" milliseconds, exceeding allowed " + milliseconds + " milliseconds.");
        }
    }

}