package cloud.filibuster.functional.java.basic.rpc;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.FilibusterGrpcBasicAnalysisConfigurationFile;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerClient;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerRule;
import com.linecorp.armeria.client.grpc.GrpcClients;
import com.linecorp.armeria.common.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Metadata.setMetadataDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.TestScope.setTestScopeCounter;
import static cloud.filibuster.junit.assertions.Helpers.testBlock;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RpcClientWithCircuitBreakerClientTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private final static Set<String> expectedExceptionsThrown = new HashSet<>();

    static {
        // Hello RPC fails.
        expectedExceptionsThrown.add("DEADLINE_EXCEEDED");
        expectedExceptionsThrown.add("UNAVAILABLE");

        // Hello RPC open circuit breaker from the test client.
        expectedExceptionsThrown.add("UNKNOWN");

        // World RPC fails.
        expectedExceptionsThrown.add("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED");
        expectedExceptionsThrown.add("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE");
    }

    @BeforeAll
    public static void setProperties() {
        setMetadataDigest(false);
        setTestScopeCounter(true);
    }

    @AfterAll
    public static void unsetProperties() {
        setMetadataDigest(true);
        setTestScopeCounter(false);
    }

    // Share CB and client across all requests and treat every single request as failure.
    // Note: all GRPC failures are 200 OK with embedded error status in header if service is online.
    //
    private final CircuitBreakerRule CIRCUIT_BREAKER_RULE = CircuitBreakerRule.builder()
            .onStatus(HttpStatus.OK)
            .thenFailure();

    private final String HELLO_SERVICE_URI = "gproto+http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";

    private final HelloServiceGrpc.HelloServiceBlockingStub HELLO_SERVICE = GrpcClients.builder(HELLO_SERVICE_URI)
            .intercept(new FilibusterClientInterceptor("test"))
            .decorator(CircuitBreakerClient.builder(CIRCUIT_BREAKER_RULE).newDecorator())
            .build(HelloServiceGrpc.HelloServiceBlockingStub.class);

    @TestWithFilibuster(analysisConfigurationFile = FilibusterGrpcBasicAnalysisConfigurationFile.class)
    @Order(1)
    public void testHello() {
        for (int i = 0; i < 3; i++) {
            testBlock(() -> {
                try {
                    Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
                    Hello.HelloReply reply = HELLO_SERVICE.partialHello(request);
                    assertEquals("Hello, Armerian World!!", reply.getMessage());
                } catch (RuntimeException e) {
                    assertTrue(wasFaultInjected());
                    assertTrue(expectedExceptionsThrown.contains(e.getMessage()), "wasn't in the expected list: " + e.getMessage());
                    testExceptionsThrown.add(e.getMessage());
                }
            });
        }
    }

    @Test
    @Order(2)
    public void testNumExceptionsThrown() {
        assertEquals(expectedExceptionsThrown.size(), testExceptionsThrown.size());
    }
}