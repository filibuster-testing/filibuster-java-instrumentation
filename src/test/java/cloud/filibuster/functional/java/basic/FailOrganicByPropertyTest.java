package cloud.filibuster.functional.java.basic;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.exceptions.filibuster.FilibusterOrganicFailuresPresentException;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.FilibusterSingleFaultUnavailableAnalysisConfigurationFile;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.grpcmock.junit5.GrpcMockExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.UUID;

import static cloud.filibuster.instrumentation.helpers.Property.setTestFailOnOrganicFailuresProperty;
import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FailOrganicByPropertyTest {
    @RegisterExtension
    static GrpcMockExtension grpcMockExtension = GrpcMockExtension.builder()
            .withPort(Networking.getPort("mock"))
            .build();

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    @BeforeAll
    public static void setProperties() {
        setTestFailOnOrganicFailuresProperty(true);
    }

    @AfterAll
    public static void resetProperties() {
        setTestFailOnOrganicFailuresProperty(false);
    }

    public static int testInvocationCount = 0;

    public static int testFailures = 0;

    @TestWithFilibuster(
            dataNondeterminism = true,
            analysisConfigurationFile = FilibusterSingleFaultUnavailableAnalysisConfigurationFile.class,
            maxIterations = 1,
            expected = FilibusterOrganicFailuresPresentException.class
    )
    @Order(1)
    public void testPurchase() {
        testInvocationCount++;

        String sessionId = UUID.randomUUID().toString();

        ManagedChannel apiChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();

        try {
            APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(apiChannel);
            Hello.PurchaseRequest request = Hello.PurchaseRequest.newBuilder().setSessionId(sessionId).build();
            Hello.PurchaseResponse response = blockingStub.simulatePurchase(request);
            assertNotNull(response);
        } catch (RuntimeException e) {
            testFailures++;
        }
    }
}
