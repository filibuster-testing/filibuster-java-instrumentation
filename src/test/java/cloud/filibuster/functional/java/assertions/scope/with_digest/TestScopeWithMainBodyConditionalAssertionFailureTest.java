package cloud.filibuster.functional.java.assertions.scope.with_digest;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.assertions.BlockType;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.TimeUnit;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.TestScope.setTestScopeCounter;
import static cloud.filibuster.junit.assertions.GrpcAssertions.tryGrpcAndCatchGrpcExceptions;
import static cloud.filibuster.junit.assertions.Helpers.executeWithoutFaultInjection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestScopeWithMainBodyConditionalAssertionFailureTest extends JUnitAnnotationBaseTest {
    @BeforeAll
    public static void setProperties() {
        setTestScopeCounter(true);
    }

    @AfterAll
    public static void resetProperties() {
        setTestScopeCounter(false);
    }

    private static int testInvocations = 0;

    private static int exceptionsThrown = 0;

    @TestWithFilibuster(
            analysisConfigurationFile = FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class,
            expected = AssertionFailedError.class
    )
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
        testInvocations++;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        executeWithoutFaultInjection(BlockType.TEST, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        });

        tryGrpcAndCatchGrpcExceptions(() -> {
            try {
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);
                assertEquals("Hello, Armerian World!!", reply.getMessage());
            } catch (RuntimeException e) {
                // Fault will be captured here, if injected.
                // Simulate an assertion failure that won't hold if fault is injected.
                // ...but, that won't explicitly throw a StatusRuntimeException.
                assertEquals("something", "something that isn't the same");
            }
        }, (t) -> {
            // Ignore the failure, don't do anything right now.
            exceptionsThrown++;

            // We should never hit this.
            assertNull(t);
        });

        executeWithoutFaultInjection(BlockType.TEST, () -> {
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
        assertEquals(2, testInvocations);
    }

    @Test
    @Order(2)
    public void verifyExceptionsThrown() {
        assertEquals(0, exceptionsThrown);
    }
}