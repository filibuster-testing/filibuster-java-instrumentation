package cloud.filibuster.functional.java.assertions;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.assertPassesAndThrowsOnlyUnderFault;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FilibusterConditionalByEnvironmentSuite
public class RENAMEMETest extends JUnitAnnotationBaseTest {
    // TODO: rename test.

    // TODO: assert proper count
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    // TODO: assert proper count
    private final static Set<String> innerTestExceptionsThrown = new HashSet<>();

    // TODO: assert proper count
    private static int invocationCount = 0;

    // TODO: assert proper count
    private static int continuationInvocationCount = 0;

    @TestWithFilibuster()
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
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
                innerTestExceptionsThrown.add(t.getMessage());
            });
        });

        // Teardown
        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }
}