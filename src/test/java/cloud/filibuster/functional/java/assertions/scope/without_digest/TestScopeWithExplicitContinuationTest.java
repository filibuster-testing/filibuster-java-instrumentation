package cloud.filibuster.functional.java.assertions.scope.without_digest;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.assertions.BlockType;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Metadata.setMetadataDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.TestScope.setTestScopeCounter;
import static cloud.filibuster.junit.assertions.GrpcAssertions.tryGrpcAndCatchGrpcExceptions;
import static cloud.filibuster.junit.assertions.Helpers.executeWithoutFaultInjection;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestScopeWithExplicitContinuationTest extends JUnitAnnotationBaseTest {
    @BeforeAll
    public static void setProperties() {
        setMetadataDigest(false);
        setTestScopeCounter(true);
    }

    @AfterAll
    public static void resetProperties() {
        setMetadataDigest(true);
        setTestScopeCounter(false);
    }

    private static int testInvocations = 0;

    private static int explicitContinutionInvocations = 0;

    private static int exceptionsThrown = 0;

    private static int continuationExceptionsThrown = 0;

    @TestWithFilibuster(
            analysisConfigurationFile = FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class
    )
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
        testInvocations++;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        AtomicBoolean completedSuccessfully = new AtomicBoolean(false);

        executeWithoutFaultInjection(BlockType.TEST, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        });

        tryGrpcAndCatchGrpcExceptions(() -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
            completedSuccessfully.set(true);
        }, (t) -> {
            // Ignore the failure, don't do anything right now.
            exceptionsThrown++;
        });

        if (completedSuccessfully.get()) {
            explicitContinutionInvocations++;

            tryGrpcAndCatchGrpcExceptions(() -> {
                HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
                Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                Hello.HelloReply reply = blockingStub.partialHello(request);
                assertEquals("Hello, Armerian World!!", reply.getMessage());
            }, (t) -> {
                // Ignore the failure, don't do anything right now.
                continuationExceptionsThrown++;
            });
        }

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
        assertEquals(3, testInvocations);
    }

    @Test
    @Order(2)
    public void verifyExplicitContinuationInvocations() {
        assertEquals(2, explicitContinutionInvocations);
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

    @Order(2)
    @Test
    public void testWarnings() {
        TestExecutionReport testExecutionReport = FilibusterCore.getMostRecentInitialTestExecutionReport();
        List<FilibusterAnalyzerWarning> warnings = testExecutionReport.getWarnings();
        assertEquals(0, warnings.size());
    }
}