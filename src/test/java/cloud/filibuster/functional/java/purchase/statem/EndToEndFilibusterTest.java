package cloud.filibuster.functional.java.purchase.statem;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.functional.java.purchase.PurchaseBaseTest;

import cloud.filibuster.functional.java.purchase.configurations.GRPCAnalysisConfigurationFile;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.PurchaseWorkflow;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.Status;
import org.grpcmock.junit5.GrpcMockExtension;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static cloud.filibuster.functional.java.purchase.statem.GrpcMock.verifyThat;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndFilibusterTest extends PurchaseBaseTest implements FilibusterTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    private final UUID sessionId = UUID.randomUUID();
    private final UUID consumerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();
    private final AtomicReference<Hello.PurchaseResponse> response = new AtomicReference<>();

    @Override
    public void failureBlock() {
        downstreamFailureResultsInException(
                UserServiceGrpc.getGetUserFromSessionMethod(),
                Status.Code.UNAVAILABLE,
                "Purchase could not be completed at this time, please retry the request: user could not be retrieved.");

        downstreamFailureResultsInException(
                CartServiceGrpc.getGetCartForSessionMethod(),
                Status.Code.UNAVAILABLE,
                "Purchase could not be completed at this time, please retry the request: cart could not be retrieved.");

        onException(Status.Code.UNAVAILABLE,
                // If we return unavailable, we should never invoke any of these downstream dependencies.
                () -> {
                    GrpcMock.adjustExpectation(CartServiceGrpc.getGetCartForSessionMethod(), 0);

                    for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
                        Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                                .setCode(discountCode.getKey())
                                .build();

                        GrpcMock.adjustExpectation(CartServiceGrpc.getGetDiscountOnCartMethod(), request, 0);
                    }
                }
        );

        onFaultOnRequest(
                CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder()
                        .setCode("FIRST-TIME")
                        .build(),
                () -> {
                    // Verify response.
                    assertNotNull(response.get());
                    assertTrue(response.get().getSuccess());
                    assertEquals("9500", response.get().getTotal());

                    // Verify cache writes.
                    JSONObject cacheObject = PurchaseWorkflow.getCacheObjectForUser(consumerId);
                    JSONObject expectedCacheObject = generateExpectedCacheObject(consumerId.toString(), cartId.toString(), "9500");
                    assertTrue(expectedCacheObject.similar(cacheObject));

                    // Verify database writes.
                    int consumerAccountBalance = PurchaseWorkflow.getAccountBalance(consumerId);
                    assertEquals(20000 - 9500, consumerAccountBalance);

                    // Verify database writes.
                    int merchantAccountBalance = PurchaseWorkflow.getAccountBalance(merchantId);
                    assertEquals(9500, merchantAccountBalance);
                }
        );

        onFaultOnRequest(
                CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder()
                        .setCode("RETURNING")
                        .build(),
                () -> {
                    // Verify response.
                    assertNotNull(response.get());
                    assertTrue(response.get().getSuccess());
                    assertEquals("9000", response.get().getTotal());

                    // Verify cache writes.
                    JSONObject cacheObject = PurchaseWorkflow.getCacheObjectForUser(consumerId);
                    JSONObject expectedCacheObject = generateExpectedCacheObject(consumerId.toString(), cartId.toString(), "9000");
                    assertTrue(expectedCacheObject.similar(cacheObject));

                    // Verify database writes.
                    int consumerAccountBalance = PurchaseWorkflow.getAccountBalance(consumerId);
                    assertEquals(20000 - 9000, consumerAccountBalance);

                    // Verify database writes.
                    int merchantAccountBalance = PurchaseWorkflow.getAccountBalance(merchantId);
                    assertEquals(9000, merchantAccountBalance);
                }
        );
    }

    @Override
    public void setupBlock() {
        // Reset cache state.
        PurchaseWorkflow.resetCacheObjectForUser(consumerId);

        // Reset database state.
        PurchaseWorkflow.depositFundsToAccount(consumerId, 20000);
        assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));

        // Reset database state.
        PurchaseWorkflow.depositFundsToAccount(merchantId, 0);
        assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));
    }

    @Override
    public void stubBlock() {
        stubFor(unaryMethod(UserServiceGrpc.getGetUserFromSessionMethod())
                .willReturn(Hello.GetUserResponse.newBuilder()
                        .setUserId(consumerId.toString())
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetCartForSessionMethod())
                .willReturn(Hello.GetCartResponse.newBuilder()
                        .setCartId(cartId.toString())
                        .setTotal("10000")
                        .setMerchantId(merchantId.toString())
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                .withRequest(Hello.GetDiscountRequest.newBuilder()
                        .setCode("FIRST-TIME")
                        .build())
                .willReturn(Hello.GetDiscountResponse.newBuilder()
                        .setPercent("10")
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                .withRequest(Hello.GetDiscountRequest.newBuilder()
                        .setCode("RETURNING")
                        .build())
                .willReturn(Hello.GetDiscountResponse.newBuilder()
                        .setPercent("5")
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                .withRequest(Hello.GetDiscountRequest.newBuilder()
                        .setCode("DAILY")
                        .build())
                .willReturn(Hello.GetDiscountResponse.newBuilder()
                        .setPercent("1")
                        .build()));
    }

    @Override
    public void testBlock() {
        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                .setSessionId(sessionId.toString())
                .build();
        response.set(blockingStub.purchase(request));
    }

    @Override
    public void assertTestBlock() {
        // Verify response.
        assertNotNull(response.get());
        assertTrue(response.get().getSuccess());
        assertEquals("9000", response.get().getTotal());

        // Verify cache writes.
        JSONObject cacheObject = PurchaseWorkflow.getCacheObjectForUser(consumerId);
        assertTrue(generateExpectedCacheObject(consumerId.toString(), cartId.toString(), "9000").similar(cacheObject));

        // Verify database writes.
        assertEquals(11000, PurchaseWorkflow.getAccountBalance(consumerId));
        assertEquals(9000, PurchaseWorkflow.getAccountBalance(merchantId));
    }

    @Override
    public void assertStubBlock() {
        verifyThat(UserServiceGrpc.getGetUserFromSessionMethod(), 1);
        verifyThat(CartServiceGrpc.getGetCartForSessionMethod(), 1);

        for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
            Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                    .setCode(discountCode.getKey())
                    .build();

            verifyThat(CartServiceGrpc.getGetDiscountOnCartMethod(), request, 1);
        }
    }

    @Override
    public void teardownBlock() {
        // Reset cache state.
        PurchaseWorkflow.resetCacheObjectForUser(consumerId);

        // Reset database state.
        PurchaseWorkflow.deleteAccount(consumerId);

        // Reset database state.
        PurchaseWorkflow.deleteAccount(merchantId);
    }

    @TestWithFilibuster(
            analysisConfigurationFile = GRPCAnalysisConfigurationFile.class,
            maxIterations = 5,
            dataNondeterminism = true,
            searchStrategy = FilibusterSearchStrategy.BFS
    )
    public void test() {
        execute();
    }
}
