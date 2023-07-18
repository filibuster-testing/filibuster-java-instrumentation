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

public class EndToEndFilibusterWithDiscountGrpcTest extends EndToEndFilibusterGrpcTest {
    // You cannot use this in a superclass because the stupid library doesn't know when to shutdown.
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
    @Override
    public void test() {
        execute();
    }
}
