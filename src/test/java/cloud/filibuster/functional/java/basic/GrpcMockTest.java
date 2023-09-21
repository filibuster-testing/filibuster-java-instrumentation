package cloud.filibuster.functional.java.basic;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;

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
import java.util.UUID;

import static cloud.filibuster.instrumentation.helpers.Property.setTestAvoidRedundantInjectionsProperty;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.assertions.protocols.GrpcAssertions.wasFaultInjectedOnMethod;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.never;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GrpcMockTest {
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

    @BeforeAll
    public static void setProperties() {
        setTestAvoidRedundantInjectionsProperty(false);
    }

    public static int testInvocationCount = 0;

    public static int testFailures = 0;

    @TestWithFilibuster(
            dataNondeterminism = true,
            analysisConfigurationFile = FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class
    )
    @Order(1)
    public void testPurchase() {
        testInvocationCount++;

        stubFor(unaryMethod(UserServiceGrpc.getGetUserMethod())
                .willReturn(Hello.GetUserResponse.newBuilder().setUserId("1").build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetCartMethod())
                .willReturn(Hello.GetCartResponse.newBuilder().setCartId("1").build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                .willReturn(Hello.GetDiscountResponse.newBuilder().build()));

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

        if (wasFaultInjectedOnMethod(CartServiceGrpc.getGetDiscountOnCartMethod())) {
            verifyThat(CartServiceGrpc.getGetDiscountOnCartMethod(), never());
        }
    }

    @Test
    @Order(2)
    public void testInvocationCount() {
        assertEquals(8, testInvocationCount);
    }

    @Test
    @Order(2)
    public void testFailures() {
        assertEquals(6, testFailures);
    }
}
