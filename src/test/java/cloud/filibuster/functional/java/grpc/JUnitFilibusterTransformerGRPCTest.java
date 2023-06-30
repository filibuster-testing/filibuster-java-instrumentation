package cloud.filibuster.functional.java.grpc;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.grpc.FilibusterGRPCNullTransformerAnalysisConfigurationFile;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTransformerGRPCTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();
    private static int numberOfExecutions = 0;

    @BeforeAll
    public static void startHelloService() throws IOException, InterruptedException {
        startHelloServerAndWaitUntilAvailable();
    }


    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(analysisConfigurationFile = FilibusterGRPCNullTransformerAnalysisConfigurationFile.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        numberOfExecutions++;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        try {
            ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("hello");
            Channel interceptedChannel = ClientInterceptors.intercept(helloChannel, clientInterceptor);

            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(interceptedChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("world").build();
            Hello.HelloReply reply = blockingStub.hello(request);
            assertEquals("Hello, world!!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected(), "An exception was thrown although no fault was injected: " + t);
            assertTrue(wasFaultInjectedOnMethod("cloud.filibuster.examples.HelloService/Hello"),
                    "Fault was not injected on the expected Redis method: " + t);
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    // 1 for the reference execution and 1 for the test with the injected Byzantine fault
    public void testNumExecutions() {
        assertEquals(2, numberOfExecutions);
    }

    @DisplayName("Verify correct faults number and message.")
    @Test
    @Order(3)
    public void testNumFaults() {
        // 1 fault for the Byzantine value
        assertEquals(1, testExceptionsThrown.size());

        // Injecting the Byzantine value 'null' for the 'name' field of the 'HelloRequest' message
        // results in the message 'Hello, !!' instead of 'Hello, world!!'
        assertTrue(testExceptionsThrown.contains("expected: <Hello, world!!> but was: <Hello, !!>"));

    }
}
