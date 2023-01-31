package cloud.filibuster.functional.python.armeria;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import cloud.filibuster.junit.server.backends.FilibusterLocalProcessServerBackend;
import cloud.filibuster.functional.JUnitBaseTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify that the Filibuster analysis can be configured to use a custom set of faults.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("Java8ApiChecker")
public class JUnitFilibusterTestWithFailFastException extends JUnitBaseTest {
    private static final String analysisFilePath = "/tmp/filibuster-custom-analysis-file";

    @BeforeAll
    public static void writeCustomAnalysisFile() {
        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)")

                // Base exceptions.
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
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "UNKNOWN"
                ))

                // Custom exception type, but must support the RuntimeException set of constructors.
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "cloud.filibuster.exceptions.CircuitBreakerException",
                        "code", "UNKNOWN"
                ))

                .build();
        FilibusterCustomAnalysisConfigurationFile filibusterAnalysisConfigurationFile = new FilibusterCustomAnalysisConfigurationFile.Builder()
                .analysisConfiguration(filibusterAnalysisConfiguration)
                .build();
        boolean result = filibusterAnalysisConfigurationFile.writeToDisk(analysisFilePath);
        assertTrue(result);
    }

    private final static Set<String> testExceptionsThrown = new HashSet<>();

    /**
     * Inject faults between Hello and World service and verify that all faults that are injected are supposed to be.
     *
     * @throws InterruptedException thrown if the gRPC channel fails to terminate.
     */
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(analysisFile=analysisFilePath, serverBackend=FilibusterLocalProcessServerBackend.class)
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        boolean expected = false;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                testExceptionsThrown.add(t.getMessage());

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: NOT_FOUND")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INVALID_ARGUMENT")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
                    expected = true;
                }

                if (t.getMessage().equals("INTERNAL: io.grpc.StatusRuntimeException: UNKNOWN")) {
                    expected = true;
                }

                if (! expected) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    /**
     * Verify that the correct number of Filibuster tests are generated.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(6, testExceptionsThrown.size());
    }
}