package cloud.filibuster.junit.tests.filibuster.server;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import cloud.filibuster.junit.tests.filibuster.JUnitBaseTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("Java8ApiChecker")
public class JUnitFilibusterTestWithExhaustiveAnalysisFile extends JUnitBaseTest {
    private static final String analysisFilePath = "/tmp/filibuster-exhaustive-analysis-file";
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();

    static {
        exhaustiveGrpcErrorCodeList.add("CANCELLED");
        exhaustiveGrpcErrorCodeList.add("UNKNOWN");
        exhaustiveGrpcErrorCodeList.add("INVALID_ARGUMENT");
        exhaustiveGrpcErrorCodeList.add("DEADLINE_EXCEEDED");
        exhaustiveGrpcErrorCodeList.add("NOT_FOUND");
        exhaustiveGrpcErrorCodeList.add("ALREADY_EXISTS");
        exhaustiveGrpcErrorCodeList.add("PERMISSION_DENIED");
        exhaustiveGrpcErrorCodeList.add("RESOURCE_EXHAUSTED");
        exhaustiveGrpcErrorCodeList.add("FAILED_PRECONDITION");
        exhaustiveGrpcErrorCodeList.add("ABORTED");
        exhaustiveGrpcErrorCodeList.add("OUT_OF_RANGE");
        exhaustiveGrpcErrorCodeList.add("UNIMPLEMENTED");
        exhaustiveGrpcErrorCodeList.add("INTERNAL");
        exhaustiveGrpcErrorCodeList.add("UNAVAILABLE");
        exhaustiveGrpcErrorCodeList.add("DATA_LOSS");
        exhaustiveGrpcErrorCodeList.add("UNAUTHENTICATED");

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*Service/.*)");

        for (String errorCode: exhaustiveGrpcErrorCodeList) {
            filibusterAnalysisConfigurationBuilder.exception("io.grpc.StatusRuntimeException", Map.of(
                    "cause", "",
                    "code", errorCode
            ));
        }

        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = filibusterAnalysisConfigurationBuilder.build();
        FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile = new FilibusterCustomAnalysisConfigurationFile.Builder()
                .analysisConfiguration(filibusterAnalysisConfiguration)
                .build();
        filibusterCustomAnalysisConfigurationFile.writeToDisk(analysisFilePath);
    }

    private static int numberOfTestsExceptionsThrownFaultsInjected = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @FilibusterTest(analysisFile=analysisFilePath)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                numberOfTestsExceptionsThrownFaultsInjected++;

                boolean found = false;

                for (String errorCode: exhaustiveGrpcErrorCodeList) {
                    String expectedString = "DATA_LOSS: io.grpc.StatusRuntimeException: " + errorCode;
                    if(t.getMessage().equals(expectedString)) {
                        found = true;
                    }
                }

                if (! found) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(16, numberOfTestsExceptionsThrownFaultsInjected);
    }
}