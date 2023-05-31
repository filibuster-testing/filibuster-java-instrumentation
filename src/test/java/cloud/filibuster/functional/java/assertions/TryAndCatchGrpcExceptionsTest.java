package cloud.filibuster.functional.java.assertions;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.assertions.Grpc.wasFaultInjectedOnService;
import static cloud.filibuster.junit.assertions.Grpc.tryGrpcAndCatchGrpcExceptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TryAndCatchGrpcExceptionsTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    private static final List<String> allowedExceptionMessages = new ArrayList<>();

    static {
        allowedExceptionMessages.add("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED");
        allowedExceptionMessages.add("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE");
        allowedExceptionMessages.add("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED");
        allowedExceptionMessages.add("DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL");
        allowedExceptionMessages.add("DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN");
    }

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster()
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
        numberOfTestExecutions++;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        tryGrpcAndCatchGrpcExceptions(() -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        }, (t) -> {
            testExceptionsThrown.add(t.getMessage());

            assertTrue(wasFaultInjected());
            assertTrue(wasFaultInjectedOnService("WorldService"));
            assertTrue(wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World"));
            assertTrue(wasFaultInjectedOnMethod(WorldServiceGrpc.getWorldMethod()));

            assertTrue(t instanceof StatusRuntimeException);

            assertTrue(allowedExceptionMessages.contains(t.getMessage()));
        });

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(6, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(5, testExceptionsThrown.size());
    }
}