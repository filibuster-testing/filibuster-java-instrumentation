package cloud.filibuster.functional.java.scope;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.assertions.Grpc;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Source.setSourceDigest;
import static cloud.filibuster.instrumentation.helpers.Property.setDeiFaultScopeCounterProperty;
import static cloud.filibuster.junit.Assertions.assertPassesAndThrowsOnlyUnderFault;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FilibusterConditionalByEnvironmentSuite
public class FaultScopeJUnitFilibusterTest extends JUnitAnnotationBaseTest {
    @BeforeAll
    public static void setProperties() {
        setSourceDigest(false);
        setDeiFaultScopeCounterProperty(true);
    }

    @AfterAll
    public static void resetProperties() {
        setSourceDigest(true);
        setDeiFaultScopeCounterProperty(false);
    }

    private static int testInvocations = 0;

    private static int continutionInvocations = 0;

    private static int exceptionsThrown = 0;

    private static int continuationExceptionsThrown = 0;

    @TestWithFilibuster(analysisConfigurationFile = FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        testInvocations++;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        Grpc.executeGrpcWithoutFaults(() -> {
            // First RPC.
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        });

        assertPassesAndThrowsOnlyUnderFault(() -> {
            // First RPC.
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        }, (t) -> {
            // Ignore the failure, don't do anything right now.
            exceptionsThrown++;
        }, () -> {
            continutionInvocations++;

            assertPassesAndThrowsOnlyUnderFault(() -> {
                // Same RPC again, new scope.
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);
                assertEquals("Hello, Armerian World!!", reply.getMessage());
            }, (t) -> {
                // Ignore the failure, don't do anything right now.
                continuationExceptionsThrown++;
            });
        });

        Grpc.executeGrpcWithoutFaults(() -> {
            // First RPC.
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        });

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @Test
    @Order(2)
    public void verifyTestInvocations() {
        assertEquals(3, testInvocations);
    }

    @Test
    @Order(2)
    public void verifyContinuationInvocations() {
        assertEquals(2, continutionInvocations);
    }

    @Test
    @Order(2)
    public void verifyExceptionsThrown() {
        assertEquals(1, exceptionsThrown);
    }

    @Test
    @Order(2)
    public void verifyContinuationExceptionsThrown() {
        assertEquals(1, continuationExceptionsThrown);
    }
}