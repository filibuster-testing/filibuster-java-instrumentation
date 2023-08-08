package cloud.filibuster.functional.docker;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.WorldServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.functional.JUnitBaseTest;
import cloud.filibuster.junit.server.backends.FilibusterDockerServerBackend;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.ContainerLaunchException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.GrpcAssertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterDockerBackendTestBadImageExpected extends JUnitBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    /**
     * Inject faults between Hello and World using Filibuster and assert proper faults are injected.
     *
     * @throws InterruptedException if teardown of gRPC channel fails.
     */
    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(serverBackend = FilibusterDockerServerBackend.class, dockerImageName = "filibustertesting/asdfasdf:0.33", expected = ContainerLaunchException.class)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        numberOfTestExecutions++;

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
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            boolean wasFaultInjected = wasFaultInjected();

            if (wasFaultInjected) {
                testExceptionsThrown.add(t.getMessage());

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                }

                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("world");
                assertTrue(wasFaultInjectedOnWorldService);

                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod(WorldServiceGrpc.getWorldMethod());
                assertTrue(wasFaultInjectedOnWorldMethod);

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
     * Verify that Filibuster generated the correct number of fault injections.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(0, testExceptionsThrown.size());
    }

    /**
     * Verify correct number of test executions.
     */
    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(0, numberOfTestExecutions);
    }
}