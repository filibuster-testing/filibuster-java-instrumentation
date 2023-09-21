package cloud.filibuster.functional.java.purchase.tests.scenarios;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.functional.java.purchase.PurchaseBaseTest;
import cloud.filibuster.functional.java.purchase.PurchaseWorkflow;
import cloud.filibuster.instrumentation.helpers.Networking;
import org.grpcmock.junit5.GrpcMockExtension;
import org.json.JSONObject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.stubFor;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;
import static org.grpcmock.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndTest extends PurchaseBaseTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    @Test
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

        // Response placeholder.
        AtomicReference<Hello.PurchaseResponse> response = new AtomicReference<>();

        // Reset cache state.
        PurchaseWorkflow.resetCacheObjectForUser(consumerId);

        // Reset database state.
        PurchaseWorkflow.depositFundsToAccount(consumerId, 20000);
        assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));

        // Reset database state.
        PurchaseWorkflow.depositFundsToAccount(merchantId, 0);
        assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));

        // ****************************************************************
        // Stub dependencies.
        // ****************************************************************

        stubFor(unaryMethod(UserServiceGrpc.getGetUserMethod())
                .willReturn(Hello.GetUserResponse.newBuilder()
                        .setUserId(consumerId.toString())
                        .build()));
        stubFor(unaryMethod(UserServiceGrpc.getValidateSessionMethod())
                .willReturn(Hello.ValidateSessionResponse.newBuilder()
                        .build()));
        stubFor(unaryMethod(CartServiceGrpc.getGetCartMethod())
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

        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                .setSessionId(sessionId.toString())
                .build();
        response.set(blockingStub.purchase(request));

        // ****************************************************************
        // Assert application invariants.
        //
        // These assertions need to be placed in a fault-free block,
        // otherwise, they might reuse code that goes through fault injection
        // instrumentation.
        // ****************************************************************

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

        // ****************************************************************
        // Assert stub invocations.
        // ****************************************************************

        verifyThat(UserServiceGrpc.getGetUserMethod(), times(1));

        verifyThat(CartServiceGrpc.getGetCartMethod(), times(1));

        for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
            verifyThat(calledMethod(CartServiceGrpc.getGetDiscountOnCartMethod())
                    .withRequest(Hello.GetDiscountRequest.newBuilder()
                            .setCode(discountCode.getKey())
                            .build()), times(1));
        }

        // ****************************************************************
        // Teardown block.
        // ****************************************************************

        // Reset cache state.
        PurchaseWorkflow.resetCacheObjectForUser(consumerId);

        // Reset database state.
        PurchaseWorkflow.deleteAccount(consumerId);

        // Reset database state.
        PurchaseWorkflow.deleteAccount(merchantId);
    }
}
