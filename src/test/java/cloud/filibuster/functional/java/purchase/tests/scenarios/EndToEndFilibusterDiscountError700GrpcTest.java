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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndToEndFilibusterDiscountError700GrpcTest extends EndToEndFilibusterGrpcTest {
    // You cannot use this in a superclass because the stupid library doesn't know when to shut down.
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    @Override
    public void failureBlock() {
        super.failureBlock();

        // Not enough of a discount if this call fails.
        assertFaultThrows(
                CartServiceGrpc.getGetDiscountOnCartMethod(),
                Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build(),
                Status.Code.FAILED_PRECONDITION,
                "Consumer did not get enough of a discount.");

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

            sideEffectingRpc(CartServiceGrpc.getNotifyDiscountAppliedMethod(), 0);
        });

        // Combination of three failures using if forces exception.
        CompositeFaultSpecification firstTwoDiscountRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build())
                .build();
        assertFaultThrows(
                firstTwoDiscountRequestsFaultSpecification,
                Status.Code.FAILED_PRECONDITION,
                "Consumer did not get enough of a discount.");

        // Combination of three failures using if forces exception.
        CompositeFaultSpecification firstAndThirdDiscountRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build())
                .build();
        assertFaultThrows(
                firstAndThirdDiscountRequestsFaultSpecification,
                Status.Code.FAILED_PRECONDITION,
                "Consumer did not get enough of a discount.");

        // Combination of three failures using if forces exception.
        CompositeFaultSpecification allDiscountRequestsFaultSpecification = new CompositeFaultSpecification.Builder()
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("FIRST-TIME").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("RETURNING").build())
                .faultOnRequest(CartServiceGrpc.getGetDiscountOnCartMethod(), Hello.GetDiscountRequest.newBuilder().setCode("DAILY").build())
                .build();
        assertFaultThrows(
                allDiscountRequestsFaultSpecification,
                Status.Code.FAILED_PRECONDITION,
                "Consumer did not get a discount.");

    }

    @Override
    public void executeTestBlock() {
        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                .setSessionId(sessionId.toString())
                .setAbortOnNoDiscount(true)
                .setAbortOnLessThanDiscountAmount(700)
                .build();
        response.set(blockingStub.purchase(request));
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
