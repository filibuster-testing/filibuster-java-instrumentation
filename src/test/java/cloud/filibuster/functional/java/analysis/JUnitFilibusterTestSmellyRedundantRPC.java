package cloud.filibuster.functional.java.analysis;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.RedundantRPCWarning;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestSmellyRedundantRPC extends JUnitAnnotationBaseTest {
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(analysisConfigurationFile=FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class, maxIterations=2)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
        Hello.HelloReply reply = blockingStub.smellyRedundantRPC(request);
        assertEquals("Hello, Smelly!", reply.getMessage());

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @Order(2)
    @Test
    public void testWarnings() {
        if (System.getenv("FILIBUSTER_DISABLED") == null) {
            TestExecutionReport testExecutionReport = FilibusterCore.getMostRecentInitialTestExecutionReport();
            List<FilibusterAnalyzerWarning> warnings = testExecutionReport.getWarnings();
            for (FilibusterAnalyzerWarning warning : warnings) {
                assertTrue(warning instanceof RedundantRPCWarning);
                assertEquals("cloud.filibuster.examples.WorldService/World", warning.getDetails());
            }
            assertEquals(3, warnings.size());
        }
    }
}