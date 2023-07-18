package cloud.filibuster.functional.java.purchase.tests;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.functional.java.purchase.PurchaseBaseTest;

import cloud.filibuster.functional.java.purchase.configurations.GRPCAnalysisConfigurationFile;
import cloud.filibuster.junit.statem.CompositeFaultSpecification;
import cloud.filibuster.junit.statem.FilibusterGrpcTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.functional.java.purchase.PurchaseWorkflow;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFilibuster;

import cloud.filibuster.junit.statem.GrpcMock;
import io.grpc.Status;
import org.grpcmock.junit5.GrpcMockExtension;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static cloud.filibuster.junit.statem.GrpcMock.stubFor;
import static cloud.filibuster.junit.statem.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndToEndFilibusterGrpcTest extends PurchaseBaseTest implements FilibusterGrpcTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    private final UUID sessionId = UUID.randomUUID();
    private final UUID consumerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();

    @Override
    public void failureBlock() {
        // Generate random so we can sample different variations of this API across
        // the different Filibuster executions.
        Random random = new Random();

        // Single faults.

        // Failure of the getUserFromSession call results in upstream receiving UNAVAILABLE exception.
        assertFaultThrows(
                UserServiceGrpc.getGetUserFromSessionMethod(),
                Status.Code.UNAVAILABLE,
                "Purchase could not be completed at this time, please retry the request: user could not be retrieved."
        );

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRPC(UserServiceGrpc.getValidateSessionMethod());
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRPC(UserServiceGrpc.getValidateSessionMethod());
            readOnlyRPC(CartServiceGrpc.getGetCartForSessionMethod());
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRPC(UserServiceGrpc.getValidateSessionMethod());
            readOnlyRPC(CartServiceGrpc.getGetCartForSessionMethod());

            for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
                Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                        .setCode(discountCode.getKey())
                        .build();
                readOnlyRPC(CartServiceGrpc.getGetDiscountOnCartMethod(), request);
            }
        });

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.UNAVAILABLE, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRPC(UserServiceGrpc.getValidateSessionMethod());
            readOnlyRPC(CartServiceGrpc.getGetCartForSessionMethod());

            for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
                Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                        .setCode(discountCode.getKey())
                        .build();
                readOnlyRPC(CartServiceGrpc.getGetDiscountOnCartMethod(), request);
            }

            sideEffectingRPC(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
        });

        // No error handling, propagate back to the upstream.
        assertFaultPropagates(UserServiceGrpc.getValidateSessionMethod());

        // State what the state of the system was on DEADLINE_EXCEEDED.
        assertOnException(Status.Code.DEADLINE_EXCEEDED, () -> {
            // Verify transaction did not occur.
            assertEquals(20000, PurchaseWorkflow.getAccountBalance(consumerId));
            assertEquals(0, PurchaseWorkflow.getAccountBalance(merchantId));

            // Notify the system some endpoints are read-only and therefore OK to skip
            // when we return a failure.
            readOnlyRPC(UserServiceGrpc.getValidateSessionMethod());
            readOnlyRPC(CartServiceGrpc.getGetCartForSessionMethod());

            for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
                Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                        .setCode(discountCode.getKey())
                        .build();
                readOnlyRPC(CartServiceGrpc.getGetDiscountOnCartMethod(), request);
            }

            sideEffectingRPC(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
        });

        // Failure of the getCartFromSession call results in upstream receiving UNAVAILABLE exception.
        assertFaultThrows(
                CartServiceGrpc.getGetCartForSessionMethod(),
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
                    CartServiceGrpc.getGetCartForSessionMethod(),
                    Hello.GetCartRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                    Status.Code.UNAVAILABLE,
                    "Purchase could not be completed at this time, please retry the request: cart could not be retrieved."
            );
        } else {
            assertFaultThrows(
                    CartServiceGrpc.getGetCartForSessionMethod(),
                    Status.Code.DEADLINE_EXCEEDED,
                    Hello.GetCartRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                    Status.Code.UNAVAILABLE,
                    "Purchase could not be completed at this time, please retry the request: cart could not be retrieved."
            );
        }

        // Failure of the first getDiscountOnCart call results in a 5% discount.
        assertOnFault(
                CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder()
                        .setCode("FIRST-TIME")
                        .build(),
                () -> { assertTestBlock(9500); }
        );

        // Failure of the second or third getDiscountOnCart call results in a 10% discount.
        // We can write this several different ways too.
        //
        // Randomly sample the two different APIs across Filibuster just as a form of
        // regression test.
        //
        if (random.nextBoolean()) {
            assertOnFault(
                    CartServiceGrpc.getGetDiscountOnCartMethod(),
                    this::assertTestBlock
            );
        } else {
            assertOnFault(
                    CartServiceGrpc.getGetDiscountOnCartMethod(),
                    Status.Code.UNAVAILABLE,
                    Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build(),
                    this::assertTestBlock
            );

            assertOnFault(
                    CartServiceGrpc.getGetDiscountOnCartMethod(),
                    Status.Code.UNAVAILABLE,
                    Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build(),
                    this::assertTestBlock
            );

            assertOnFault(
                    CartServiceGrpc.getGetDiscountOnCartMethod(),
                    Status.Code.DEADLINE_EXCEEDED,
                    this::assertTestBlock
            );
        }

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

        // Multiple faults.

        // Failure of the first two getDiscountOnCart calls results in a 1% discount.
        CompositeFaultSpecification firstTwoCartRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build())
                .build();
        assertOnFaults(firstTwoCartRequestsFaultSpecification, () -> { assertTestBlock(9900); });

        // Failure of the first and third getDiscountOnCart calls results in a 5% discount.
        CompositeFaultSpecification firstAndThirdCartRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build())
                .build();
        assertOnFaults(firstAndThirdCartRequestsFaultSpecification, () -> { assertTestBlock(9500); });

        // Failure of the second and third getDiscountOnCart calls results in a 10% discount.
        CompositeFaultSpecification secondAndThirdCartRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build())
                .build();
        assertOnFaults(secondAndThirdCartRequestsFaultSpecification, () -> { assertTestBlock(9000); });

        // Failure of all getDiscountOnCart calls results in no discount.
        CompositeFaultSpecification allCartRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build())
                .build();
        assertOnFaults(
                allCartRequestsFaultSpecification,
                () -> {
                    assertTestBlock(10000);
                    GrpcMock.adjustExpectation(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
                });
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
        stubFor(UserServiceGrpc.getValidateSessionMethod(),
                Hello.ValidateSessionRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                Hello.ValidateSessionResponse.newBuilder().build());

        stubFor(UserServiceGrpc.getGetUserFromSessionMethod(),
                Hello.GetUserRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                Hello.GetUserResponse.newBuilder().setUserId(consumerId.toString()).build());

        stubFor(CartServiceGrpc.getGetCartForSessionMethod(),
                Hello.GetCartRequest.newBuilder().setSessionId(sessionId.toString()).build(),
                Hello.GetCartResponse.newBuilder()
                        .setCartId(cartId.toString())
                        .setTotal("10000")
                        .setMerchantId(merchantId.toString())
                        .build());

        stubFor(CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build(),
                Hello.GetDiscountResponse.newBuilder().setPercent("10").build());

        stubFor(CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build(),
                Hello.GetDiscountResponse.newBuilder().setPercent("5").build());

        stubFor(CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build(),
                Hello.GetDiscountResponse.newBuilder().setPercent("1").build());

        stubFor(CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build(),
                Hello.NotifyDiscountAppliedResponse.newBuilder().build());
    }

    @Override
    public void executeTestBlock() {
        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                .setSessionId(sessionId.toString())
                .build();
        response.set(blockingStub.purchase(request));
    }

    private void assertTestBlock(int total) {
        Hello.PurchaseResponse response = (Hello.PurchaseResponse) getResponse();

        // Verify response.
        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(String.valueOf(total), response.getTotal());

        // Verify cache writes.
        JSONObject cacheObject = PurchaseWorkflow.getCacheObjectForUser(consumerId);
        assertTrue(generateExpectedCacheObject(consumerId.toString(), cartId.toString(), String.valueOf(total)).similar(cacheObject));

        // Verify database writes.
        assertEquals(20000 - total, PurchaseWorkflow.getAccountBalance(consumerId));
        assertEquals(total, PurchaseWorkflow.getAccountBalance(merchantId));
    }

    @Override
    public void assertTestBlock() {
        assertTestBlock(9000);
    }

    @Override
    public void assertStubBlock() {
        verifyThat(UserServiceGrpc.getValidateSessionMethod(), 1);
        verifyThat(UserServiceGrpc.getGetUserFromSessionMethod(), 1);
        verifyThat(CartServiceGrpc.getGetCartForSessionMethod(), 1);

        for (Map.Entry<String, String> discountCode : PurchaseWorkflow.getDiscountCodes()) {
            Hello.GetDiscountRequest request = Hello.GetDiscountRequest.newBuilder()
                    .setCode(discountCode.getKey())
                    .build();

            verifyThat(CartServiceGrpc.getGetDiscountOnCartMethod(), request, 1);
        }

        verifyThat(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 1);
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
            abortOnFirstFailure = true,
            maxIterations = 100,
            dataNondeterminism = true,
            searchStrategy = FilibusterSearchStrategy.BFS
    )
    public void test() {
        execute();
    }
}
