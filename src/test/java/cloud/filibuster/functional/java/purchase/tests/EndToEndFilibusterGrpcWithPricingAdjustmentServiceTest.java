package cloud.filibuster.functional.java.purchase.tests;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.PricingAdjustmentServiceGrpc;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.functional.java.purchase.PurchaseBaseTest;
import cloud.filibuster.functional.java.purchase.PurchaseWorkflowWithPricingAdjustmentService;
import cloud.filibuster.junit.statem.FilibusterGrpcTest;
import io.grpc.Status;
import org.json.JSONObject;

import java.util.Random;
import java.util.UUID;

import static cloud.filibuster.junit.statem.GrpcMock.stubFor;
import static cloud.filibuster.junit.statem.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndFilibusterGrpcWithPricingAdjustmentServiceTest extends PurchaseBaseTest implements FilibusterGrpcTest {
    public final UUID sessionId = UUID.randomUUID();
    protected final UUID consumerId = UUID.randomUUID();
    protected final UUID merchantId = UUID.randomUUID();
    public final UUID cartId = UUID.randomUUID();

    @Override
    public void failureBlock() {
        // Generate random so we can sample different variations of this API across
        // the different Filibuster executions.
        Random random = new Random();

        // Single faults.

        // Failure of the getUserFromSession call results in upstream receiving UNAVAILABLE exception.
        assertFaultThrows(
                UserServiceGrpc.getGetUserMethod(),
                Status.Code.UNAVAILABLE,
                "Purchase could not be completed at this time, please retry the request: user could not be retrieved."
        );

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRpc(CartServiceGrpc.getGetCartMethod());
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRpc(CartServiceGrpc.getGetCartMethod());

            Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                    .setCode(PurchaseWorkflowWithPricingAdjustmentService.getDiscountCode().getKey())
                    .build();
            readOnlyRpc(PricingAdjustmentServiceGrpc.getGetAdjustmentMethod(), request);
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRpc(CartServiceGrpc.getGetCartMethod());

            Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                    .setCode(PurchaseWorkflowWithPricingAdjustmentService.getDiscountCode().getKey())
                    .build();
            readOnlyRpc(PricingAdjustmentServiceGrpc.getGetAdjustmentMethod(), request);

            sideEffectingRpc(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
        });

        // No error handling, propagate back to the upstream.

        // State what the state of the system was on DEADLINE_EXCEEDED.
        assertOnException(Status.Code.DEADLINE_EXCEEDED, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRpc(CartServiceGrpc.getGetCartMethod());

            Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                    .setCode(PurchaseWorkflowWithPricingAdjustmentService.getDiscountCode().getKey())
                    .build();
            readOnlyRpc(PricingAdjustmentServiceGrpc.getGetAdjustmentMethod(), request);

            sideEffectingRpc(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
        });

        // Failure of the getCartFromSession call results in upstream receiving UNAVAILABLE exception.
        assertFaultThrows(
                CartServiceGrpc.getGetCartMethod(),
                Status.Code.UNAVAILABLE,
                Status.Code.UNAVAILABLE,
                "Purchase could not be completed at this time, please retry the request: cart could not be retrieved."
        );

        // We can use two different variations for the DEADLINE_EXCEEDED exception.
        // The first, will catch all exceptions because it's by request, the second by error code.
        //
        // Randomly sample the two different APIs across Filibuster just as a form of
        // regression test.
        //
        if (random.nextBoolean()) {
            assertFaultThrows(
                    CartServiceGrpc.getGetCartMethod(),
                    Hello.GetCartRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                    Status.Code.UNAVAILABLE,
                    "Purchase could not be completed at this time, please retry the request: cart could not be retrieved."
            );
        } else {
            assertFaultThrows(
                    CartServiceGrpc.getGetCartMethod(),
                    Status.Code.DEADLINE_EXCEEDED,
                    Hello.GetCartRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                    Status.Code.UNAVAILABLE,
                    "Purchase could not be completed at this time, please retry the request: cart could not be retrieved."
            );
        }

        // Failure of the getDiscount call results in no discount.
        assertOnFault(
                PricingAdjustmentServiceGrpc.getGetAdjustmentMethod(),
                Hello.GetDiscountRequest.newBuilder()
                        .setCode("FIRST-TIME")
                        .build(),
                () -> {
                    assertTestBlock(10000);
                    sideEffectingRpc(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
                }
        );

        // Failure of the getNotifyDiscountAppliedMethod has no effect.
        // We can write this several different ways too.
        //
        // Randomly sample the two different APIs across Filibuster just as a form of
        // regression test.
        //
        boolean bool1 = random.nextBoolean();
        boolean bool2 = random.nextBoolean();

        if (bool1 && bool2) {
            assertFaultHasNoImpact(
                    CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                    Status.Code.UNAVAILABLE,
                    Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build());
            assertFaultHasNoImpact(
                    CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                    Status.Code.DEADLINE_EXCEEDED);
        } else if (bool1 || bool2) {
            assertFaultHasNoImpact(
                    CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                    Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build());
        } else {
            assertFaultHasNoImpact(CartServiceGrpc.getNotifyDiscountAppliedMethod());
        }
    }

    @Override
    public void setupBlock() {
        // Reset cache state.
        PurchaseWorkflowWithPricingAdjustmentService.resetCacheObjectForUser(consumerId);

        // Reset database state.
        PurchaseWorkflowWithPricingAdjustmentService.depositFundsToAccount(consumerId, 20000);
        assertEquals(20000, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));

        // Reset database state.
        PurchaseWorkflowWithPricingAdjustmentService.depositFundsToAccount(merchantId, 0);
        assertEquals(0, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));
    }

    @Override
    public void stubBlock() {
        stubFor(UserServiceGrpc.getGetUserMethod(),
                Hello.GetUserRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                Hello.GetUserResponse.newBuilder().setUserId(consumerId.toString()).build());

        stubFor(CartServiceGrpc.getGetCartMethod(),
                Hello.GetCartRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                Hello.GetCartResponse.newBuilder()
                        .setCartId(cartId.toString())
                        .setTotal("10000")
                        .setMerchantId(merchantId.toString())
                        .build());

        stubFor(PricingAdjustmentServiceGrpc.getGetAdjustmentMethod(),
                Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build(),
                Hello.GetDiscountResponse.newBuilder().setPercent("10").build());

        stubFor(CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build(),
                Hello.NotifyDiscountAppliedResponse.newBuilder().build());
    }

    @Override
    public void executeTestBlock() {
        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                .setSessionId(sessionId.toString())
                .setAbortOnNoDiscount(false)
                .build();
        response.set(blockingStub.purchase(request));
    }

    public void assertTestBlock(int total) {
        Hello.PurchaseResponse response = (Hello.PurchaseResponse) getResponse();

        // Verify response.
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(String.valueOf(total), response.getTotal());

        // Verify cache writes.
        JSONObject cacheObject = PurchaseWorkflowWithPricingAdjustmentService.getCacheObjectForUser(consumerId);
        assertTrue(generateExpectedCacheObject(consumerId.toString(), cartId.toString(), String.valueOf(total)).similar(cacheObject));

        // Verify database writes.
        assertEquals(20000 - total, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(consumerId));
        assertEquals(total, PurchaseWorkflowWithPricingAdjustmentService.getAccountBalance(merchantId));
    }

    @Override
    public void assertTestBlock() {
        assertTestBlock(9000);
    }

    @Override
    public void assertStubBlock() {
        verifyThat(UserServiceGrpc.getGetUserMethod(), 1);
        verifyThat(CartServiceGrpc.getGetCartMethod(), 1);

        Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                .setCode(PurchaseWorkflowWithPricingAdjustmentService.getDiscountCode().getKey())
                .build();

        verifyThat(PricingAdjustmentServiceGrpc.getGetAdjustmentMethod(), request, 1);

        verifyThat(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 1);
    }

    @Override
    public void teardownBlock() {
        // Reset cache state.
        PurchaseWorkflowWithPricingAdjustmentService.resetCacheObjectForUser(consumerId);

        // Reset database state.
        PurchaseWorkflowWithPricingAdjustmentService.deleteAccount(consumerId);

        // Reset database state.
        PurchaseWorkflowWithPricingAdjustmentService.deleteAccount(merchantId);
    }
}
