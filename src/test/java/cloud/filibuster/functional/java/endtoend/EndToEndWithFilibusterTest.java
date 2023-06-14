package cloud.filibuster.functional.java.endtoend;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.postgresql.PurchaseWorkflow;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.PurchaseCustomAnalysisConfigurationFile;
import org.grpcmock.junit5.GrpcMockExtension;
import org.json.JSONObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static cloud.filibuster.junit.assertions.Helpers.assertionBlock;
import static cloud.filibuster.junit.assertions.Helpers.setupBlock;
import static cloud.filibuster.junit.assertions.Helpers.teardownBlock;
import static cloud.filibuster.junit.assertions.Helpers.testBlock;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndWithFilibusterTest extends BaseTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    private static final UUID SESSION_ID = UUID.randomUUID();

    private static final UUID CONSUMER_UUID = UUID.randomUUID();

    private static final UUID MERCHANT_UUID = UUID.randomUUID();

    private static final UUID CART_UUID = UUID.randomUUID();

    private static JSONObject generateExpectedCacheObject() {
        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("cart_id", CART_UUID.toString());
        expectedJsonObject.put("user_id", CONSUMER_UUID.toString());
        expectedJsonObject.put("purchased", true);
        expectedJsonObject.put("total", "9000");
        return expectedJsonObject;
    }

    @TestWithFilibuster(
            analysisConfigurationFile = PurchaseCustomAnalysisConfigurationFile.class,
            maxIterations = 1,
            suppressCombinations = true
    )
    public void testEndToEnd() {
        // ****************************************************************
        // Setup state.
        //
        // No need for fault-free block if done in BeforeEach, as
        // that will implicitly disable fault injection.
        // ****************************************************************

        setupBlock(() -> {
            // Reset cache state.
            PurchaseWorkflow.resetCacheObjectForUser(CONSUMER_UUID);

            // Reset database state.
            PurchaseWorkflow.depositFundsToAccount(CONSUMER_UUID, 20000);
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(CONSUMER_UUID));

            PurchaseWorkflow.depositFundsToAccount(MERCHANT_UUID, 0);
            assertEquals(0, PurchaseWorkflow.getAccountBalance(MERCHANT_UUID));
        });

        // ****************************************************************
        // Stub dependencies.
        // ****************************************************************

        stubFor(unaryMethod(UserServiceGrpc.getGetUserFromSessionMethod())
                .willReturn(Hello.GetUserResponse.newBuilder()
                        .setUserId(CONSUMER_UUID.toString())
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetCartForSessionMethod())
                .willReturn(Hello.GetCartResponse.newBuilder()
                        .setCartId(CART_UUID.toString())
                        .setTotal("10000")
                        .setMerchantId(MERCHANT_UUID.toString())
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                .willReturn(Hello.GetDiscountResponse.newBuilder()
                        .setPercent("10")
                        .build()));

        // ****************************************************************
        // Issue RPC and assert on the response.
        // ****************************************************************

        testBlock(() -> {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
            Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                    .setSessionId(SESSION_ID.toString())
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
            JSONObject cacheObject = PurchaseWorkflow.getCacheObjectForUser(CONSUMER_UUID);
            assertTrue(generateExpectedCacheObject().similar(cacheObject));

            // Verify database writes.
            assertEquals(11000, PurchaseWorkflow.getAccountBalance(CONSUMER_UUID));
            assertEquals(9000, PurchaseWorkflow.getAccountBalance(MERCHANT_UUID));
        });

        // ****************************************************************
        // Assert stub invocations.
        // ****************************************************************

        verifyThat(UserServiceGrpc.getGetUserFromSessionMethod(), times(4));
        verifyThat(CartServiceGrpc.getGetCartForSessionMethod(), times(1));
        verifyThat(CartServiceGrpc.getGetDiscountOnCartMethod(), times(1));

        // ****************************************************************
        // Assert stub invocations.
        // ****************************************************************

        teardownBlock(() -> {
            // Reset cache state.
            PurchaseWorkflow.resetCacheObjectForUser(CONSUMER_UUID);

            // Reset database state.
            PurchaseWorkflow.deleteAccount(CONSUMER_UUID);
            PurchaseWorkflow.deleteAccount(MERCHANT_UUID);
        });
    }
}
