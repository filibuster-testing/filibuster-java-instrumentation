package cloud.filibuster.functional.java.redundant;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.FilibusterAnalyzerWarning;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.RedundantRPCWarning;
import cloud.filibuster.junit.server.core.lint.analyzers.warnings.UnimplementedFailuresWarning;
import cloud.filibuster.junit.server.core.reports.TestExecutionReport;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedundantByAnnotationTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    public static int testInvocationCount = 0;

    public static int testFailures = 0;

    @TestWithFilibuster(
            dataNondeterminism = true,
            analysisConfigurationFile = FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class,
            avoidRedundantInjections = true
    )
    @Order(1)
    public void testPurchase() {
        testInvocationCount++;

        stubFor(unaryMethod(UserServiceGrpc.getGetUserFromSessionMethod())
                .willReturn(Hello.GetUserResponse.newBuilder().setUserId("1").build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetCartForSessionMethod())
                .willReturn(Hello.GetCartResponse.newBuilder().setCartId("1").setTotal("100.00").build()));

        String sessionId = UUID.randomUUID().toString();

        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder().setSessionId(sessionId).build();
            Hello.PurchaseResponse response = blockingStub.simulatePurchase(request);
            assertNotNull(response);
        } catch (RuntimeException e) {
            testFailures++;
        }
    }

    @Test
    @Order(2)
    public void testInvocationCount() {
        // 5 RPCs, 3 duplicates removed.
        // 3 tests failing each RPC once + 1 golden path (with UNIMPLEMENTED for final call.)
        // 4 total.
        assertEquals(4, testInvocationCount);
    }

    @Test
    @Order(2)
    public void testFailures() {
        // 5 RPCs, 3 duplicates removed.
        // 4 executions.
        // 1 non-fatal, 2 fatal.
        // 2 total.
        assertEquals(2, testFailures);
    }

    @Order(2)
    @Test
    public void testWarnings() {
        TestExecutionReport testExecutionReport = FilibusterCore.getMostRecentInitialTestExecutionReport();
        List<FilibusterAnalyzerWarning> warnings = testExecutionReport.getWarnings();
        for (FilibusterAnalyzerWarning warning : warnings) {
            String warningDetails = warning.getDetails();
            switch (warningDetails) {
                case "cloud.filibuster.examples.UserService/GetUserFromSession":
                    assertTrue(warning instanceof RedundantRPCWarning);
                    break;
                case "cloud.filibuster.examples.CartService/GetDiscountOnCart":
                    assertTrue(warning instanceof UnimplementedFailuresWarning);
                    break;
                default:
                    fail();
            }
        }

        // 4 warnings:
        // - 3 redundant, only removable through use of the property and not annotation.
        // - 1 unimplemented, for the set discount RPC.
        assertEquals(4, warnings.size());
    }
}
