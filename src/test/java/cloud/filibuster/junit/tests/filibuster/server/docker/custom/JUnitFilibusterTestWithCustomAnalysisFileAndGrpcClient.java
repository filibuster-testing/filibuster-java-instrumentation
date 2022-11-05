package cloud.filibuster.junit.tests.filibuster.server.docker.custom;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import cloud.filibuster.junit.tests.filibuster.JUnitBaseTest;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify, using instrumented GrpcClient, fault injections between test and Hello and Hello and World using
 * a custom analysis file generated for Filibuster.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("Java8ApiChecker")
public class JUnitFilibusterTestWithCustomAnalysisFileAndGrpcClient extends JUnitBaseTest {
    private static final String analysisFilePath = "/tmp/filibuster-custom-analysis-file";

    static {
        @SuppressWarnings("Java8ApiChecker")
        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*Service/.*)")
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "UNAVAILABLE"
                ))
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "DEADLINE_EXCEEDED"
                ))
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "INVALID_ARGUMENT"
                ))
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "NOT_FOUND"
                ))
                .build();
        FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile = new FilibusterCustomAnalysisConfigurationFile.Builder()
                .analysisConfiguration(filibusterAnalysisConfiguration)
                .build();
        filibusterCustomAnalysisConfigurationFile.writeToDisk(analysisFilePath);
    }

    private static int numberOfTestsExceptionsThrownFaultsInjected = 0;

    private GrpcClientBuilder grpcClientBuilder;

    @BeforeEach
    protected void setupGrpcClientBuilder() {
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        grpcClientBuilder = TestHelper.getGrpcClientBuilder(baseURI, "junit");
    }

    /**
     * Verify, using instrumented GrpcClient, fault injections between test and Hello and Hello and World using
     * a custom analysis file generated for Filibuster.
     *
     * @throws InterruptedException thrown when the gRPC channel fails to terminate.
     */
    @DisplayName("Test partial hello server grpc route with Filibuster with instrumented test client. (MyHelloService, MyWorldService)")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @FilibusterTest(analysisFile=analysisFilePath)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        boolean expected = false;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = grpcClientBuilder
                    .build(HelloServiceGrpc.HelloServiceBlockingStub.class);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                numberOfTestsExceptionsThrownFaultsInjected++;

                // Errors from the client to the Hello Service.

                if (t.getMessage().equals("NOT_FOUND: Injected fault from Filibuster, status code: 5")) {
                    expected = true;
                }

                if (t.getMessage().equals("INVALID_ARGUMENT: Injected fault from Filibuster, status code: 3")) {
                    expected = true;
                }

                if (t.getMessage().contains("DEADLINE_EXCEEDED: Injected fault from Filibuster, status code: 4")) {
                    expected = true;
                }

                if (t.getMessage().equals("UNAVAILABLE: Injected fault from Filibuster, status code: 14")) {
                    expected = true;
                }

                // Errors from the Hello to the World service.

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: NOT_FOUND")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INVALID_ARGUMENT")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }
    }

    /**
     * Verify the correct number of Filibuster tests are executed.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(8, numberOfTestsExceptionsThrownFaultsInjected);
    }
}