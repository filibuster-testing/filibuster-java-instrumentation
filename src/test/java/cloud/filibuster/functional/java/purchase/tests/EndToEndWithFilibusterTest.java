package cloud.filibuster.functional.java.purchase.tests;

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
import org.grpcmock.junit5.GrpcMockExtension;
import org.json.JSONObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static cloud.filibuster.junit.assertions.Helpers.assertionBlock;
import static cloud.filibuster.junit.assertions.Helpers.setupBlock;
import static cloud.filibuster.junit.assertions.Helpers.teardownBlock;
import static cloud.filibuster.junit.assertions.Helpers.testBlock;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndWithFilibusterTest extends PurchaseBaseTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    private static JSONObject generateExpectedCacheObject(String consumerId, String cartId) {
        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("cart_id", cartId);
        expectedJsonObject.put("user_id", consumerId);
        expectedJsonObject.put("purchased", true);
        expectedJsonObject.put("total", "9000");
        return expectedJsonObject;
    }

    @TestWithFilibuster(
            analysisConfigurationFile = GRPCAnalysisConfigurationFile.class,
            maxIterations = 1,
            suppressCombinations = true,
            dataNondeterminism = true,
            searchStrategy = FilibusterSearchStrategy.BFS
    )
    public void testEndToEnd() {
        // ****************************************************************
        // Setup state.
        //
        // No need for fault-free block if done in BeforeEach, as
        // that will implicitly disable fault injection.
        // ****************************************************************

        // Generate identifiers to use for this test.
        UUID sessionId = UUID.randomUUID();
        UUID consumerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();

        setupBlock(() -> {
            // Reset cache state.
            PurchaseWorkflow.resetCacheObjectForUser(consumerId);

            // Reset database state.
            PurchaseWorkflow.depositFundsToAccount(consumerId, 20000);
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));

            PurchaseWorkflow.depositFundsToAccount(merchantId, 0);
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));
        });

        // ****************************************************************
        // Stub dependencies.
        // ****************************************************************

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

        for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
            stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                    .withRequest(Hello.GetDiscountRequest.newBuilder()
                            .setCode(discountCode.getKey())
                            .build())
                    .willReturn(Hello.GetDiscountResponse.newBuilder()
                            .setPercent(discountCode.getValue())
                            .build()));
        }

        // ****************************************************************
        // Issue RPC and assert on the response.
        // ****************************************************************

        testBlock(() -> {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
            Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                    .setSessionId(sessionId.toString())
                    .build();
            Hello.PurchaseResponse response = blockingStub.purchase(request);
            assertNotNull(response);
            assertTrue(response.getSuccess());
            assertEquals("9000", response.getTotal());
        });

        // ****************************************************************
        // Assert application invariants.
        //
        // These assertions need to be placed in a fault-free block,
        // otherwise, they might reuse code that goes through fault injection
        // instrumentation.
        // ****************************************************************

        assertionBlock(() -> {
            // Verify cache writes.
            JSONObject cacheObject = PurchaseWorkflow.getCacheObjectForUser(consumerId);
            assertTrue(generateExpectedCacheObject(consumerId.toString(), cartId.toString()).similar(cacheObject));

            // Verify database writes.
            assertEquals(11000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(9000, PurchaseWorkflow.getAccountBalance(merchantId));
        });

        // ****************************************************************
        // Assert stub invocations.
        // ****************************************************************

        verifyThat(UserServiceGrpc.getGetUserFromSessionMethod(), times(1));

        verifyThat(CartServiceGrpc.getGetCartForSessionMethod(), times(1));

        for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
            verifyThat(calledMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                    .withRequest(Hello.GetDiscountRequest.newBuilder()
                            .setCode(discountCode.getKey())
                            .build()), times(1));
        }

        // ****************************************************************
        // Assert stub invocations.
        // ****************************************************************

        teardownBlock(() -> {
            // Reset cache state.
            PurchaseWorkflow.resetCacheObjectForUser(consumerId);

            // Reset database state.
            PurchaseWorkflow.deleteAccount(consumerId);
            PurchaseWorkflow.deleteAccount(merchantId);
        });
    }
}
