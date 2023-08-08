package cloud.filibuster.functional.java.latency;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.exceptions.filibuster.FilibusterAllowedTimeExceededException;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.server.latency.Filibuster1000msLatencyProfile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestWithLatencyProfile extends JUnitAnnotationBaseTest {
    private static int numberOfTestsExecuted = 0;

    private static int numberOfExceptions = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(latencyProfile=Filibuster1000msLatencyProfile.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        numberOfTestsExecuted++;

        try {
            assertPassesWithinMsOrThrowsUnderFault(1, StatusRuntimeException.class, () -> {
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);
                assertEquals("Hello, Armerian World!!", reply.getMessage());
                assertFalse(wasFaultInjected());
            }, (e) -> {
                // Nothing.
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
        assertEquals(6, numberOfTestsExecuted);
    }

    @Test
    @Order(2)
    public void testNumberOfExceptions() {
        assertEquals(1, numberOfExceptions);
    }

    private static void assertPassesWithinMsOrThrowsUnderFault(int milliseconds, Class<? extends Throwable> throwable, Runnable testBlock, ThrowingConsumer<Throwable> assertionBlock) throws Throwable {
        try {
            long startTime = System.nanoTime();
            testBlock.run();
            long endTime = System.nanoTime();

            long duration = (endTime - startTime);
            long durationMs = duration / 1000000;
            if (durationMs > milliseconds) {
                throw new FilibusterAllowedTimeExceededException("Test completed in " + durationMs +" milliseconds, exceeding allowed " + milliseconds + " milliseconds.");
            }
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                if (!throwable.isInstance(t)) {
                    // Test threw, we didn't expect it: throw.
                    throw t;
                }

                // Test threw, we expected it: now check the conditional, user-provided, assertions.
                assertionBlock.accept(t);
            } else {
                // Test threw, we didn't inject a fault: throw.
                throw t;
            }
        }
    }
}