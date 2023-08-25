package cloud.filibuster.functional.java.basic.rpc;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerClient;
import com.linecorp.armeria.client.circuitbreaker.CircuitBreakerRule;
import com.linecorp.armeria.client.grpc.GrpcClients;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RpcClientTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    @TestWithFilibuster
    @Order(1)
    public void testHello() {
        String helloServiceUri = "gproto+http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        HelloServiceGrpc.HelloServiceBlockingStub helloService = GrpcClients.builder(helloServiceUri).build(HelloServiceGrpc.HelloServiceBlockingStub.class);

        try {
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = helloService.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        } catch (RuntimeException e) {
            assertTrue(wasFaultInjected());
            testExceptionsThrown.add(e.getMessage());
        }
    }

    @Test
    @Order(2)
    public void testNumExceptionsThrown() {
        assertEquals(5, testExceptionsThrown.size());
    }
}