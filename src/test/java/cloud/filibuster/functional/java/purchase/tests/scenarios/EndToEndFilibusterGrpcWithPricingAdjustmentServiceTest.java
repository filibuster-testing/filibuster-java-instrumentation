package cloud.filibuster.functional.java.purchase.tests.scenarios;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.purchase.configurations.GRPCAnalysisConfigurationFile;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.TestWithFilibuster;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EndToEndFilibusterGrpcWithPricingAdjustmentServiceTest extends cloud.filibuster.functional.java.purchase.tests.EndToEndFilibusterGrpcWithPricingAdjustmentServiceTest {
    // You cannot use this in a superclass because the stupid library doesn't know when to shut down.
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    @Override
    public void executeTestBlock() {
        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(API_CHANNEL);
        Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder()
                .setSessionId(sessionId.toString())
                .setAbortOnNoDiscount(false)
                .setAbortOnLessThanDiscountAmount(0)
                .build();
        response.set(blockingStub.makePurchase(request));
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
