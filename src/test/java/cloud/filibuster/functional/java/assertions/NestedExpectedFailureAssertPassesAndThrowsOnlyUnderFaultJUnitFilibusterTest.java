package cloud.filibuster.functional.java.assertions;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.assertPassesAndThrowsOnlyUnderFault;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FilibusterConditionalByEnvironmentSuite
public class NestedExpectedFailureAssertPassesAndThrowsOnlyUnderFaultJUnitFilibusterTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private final static Set<String> testInnerExceptionsThrown = new HashSet<>();

    private static int invocationCount = 0;

    private static int continuationInvocationCount = 0;

    private static int testInvocationCount = 0;

    private static int reachedEndOfTestWithoutThrowCount = 0;

    @TestWithFilibuster(expected=StatusRuntimeException.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
        testInvocationCount++;

        // Setup
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        // Test.
        assertPassesAndThrowsOnlyUnderFault(() -> {
            invocationCount++;

            try {
                // Test
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);

                // Assertions
                assertEquals("Hello, Armerian World!!", reply.getMessage());
            } catch (Throwable t) {
                // Ignore all failures, to ensure we never hits the exception handler.
                testExceptionsThrown.add(t.getMessage());
            }
        }, (t) -> {
            throw t;
        }, () -> {
            continuationInvocationCount++;

            assertPassesAndThrowsOnlyUnderFault(() -> {
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);

                // Assertions
                assertEquals("Hello, Armerian World!!", reply.getMessage());
            }, (t) -> {
                testInnerExceptionsThrown.add(t.getMessage());
                throw t;
            });
        });

        reachedEndOfTestWithoutThrowCount++;

        // Teardown
        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    // Verify we only reach the end of the test once and we throw.

    @Test
    @Order(2)
    public void testReachedEndOfTestWithoutThrowCount() {
        assertEquals(1, reachedEndOfTestWithoutThrowCount);
    }

    // Throw at each callsite 5 times - 5 for the latter, 5 for the former, always execute both RPCs.

    @Test
    @Order(2)
    public void testInnerExceptionsThrown() {
        assertEquals(5, testInnerExceptionsThrown.size());
    }

    @Test
    @Order(2)
    public void testExceptionsThrown() {
        assertEquals(5, testExceptionsThrown.size());
    }

    // Enter each block the same amount of times: 6 (5 faults + 1 fault-free.)

    @Test
    @Order(2)
    public void testInvocationCount() {
        assertEquals(6, invocationCount);
    }

    @Test
    @Order(2)
    public void testContinationInvocationCount() {
        assertEquals(6, continuationInvocationCount);
    }

    // We only execute the test a total of 6 times (5 faults, for each RPC + 1 fault-free.)

    @Test
    @Order(2)
    public void testTestInvocationCount() {
        assertEquals(6, testInvocationCount);
    }
}