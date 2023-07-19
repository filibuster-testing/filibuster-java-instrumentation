package cloud.filibuster.functional.java.purchase.tests.scenarios;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.CartServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.UserServiceGrpc;
import cloud.filibuster.functional.java.purchase.PurchaseWorkflow;
import cloud.filibuster.functional.java.purchase.configurations.GRPCAnalysisConfigurationFile;
import cloud.filibuster.functional.java.purchase.tests.EndToEndFilibusterGrpcTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.statem.CompositeFaultSpecification;
import io.grpc.Status;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static cloud.filibuster.junit.statem.GrpcMock.stubFor;
import static cloud.filibuster.junit.statem.GrpcMock.verifyThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndToEndFilibusterDiscountError50GrpcTest extends EndToEndFilibusterGrpcTest {
    // You cannot use this in a superclass because the stupid library doesn't know when to shut down.
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    @Override
    public void failureBlock() {
        super.failureBlock();

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

            sideEffectingRPC(
                    CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                    Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build(),
                    0);
        });

        // State what the state of the system was on UNAVAILABLE.
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

            sideEffectingRPC(
                    CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                    Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build(),
                    0);
        });

        // Combination of three failures using if forces exception.
        CompositeFaultSpecification allCartRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build())
                .build();
        assertFaultThrows(
                allCartRequestsFaultSpecification,
                Status.Code.FAILED_PRECONDITION,
                "Consumer did not get a discount.");

        // State what the state of the system was on UNAVAILABLE.
        assertOnException(Status.Code.FAILED_PRECONDITION, () -> {
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

            sideEffectingRPC(
                    CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                    Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build(),
                    0);
        });
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
                .setAbortOnNoDiscount(true)
                .setAbortOnLessThanDiscountAmount(50)
                .build();
        response.set(blockingStub.purchase(request));
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

        verifyThat(
                CartServiceGrpc.getNotifyDiscountAppliedMethod(),
                Hello.NotifyDiscountAppliedRequest.newBuilder().setCartId(cartId.toString()).build(),
                1);
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
