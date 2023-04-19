package cloud.filibuster.functional.java.bfs;

import cloud.filibuster.examples.Hello.HelloReply;
import cloud.filibuster.examples.Hello.HelloRequest;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.examples.HelloServiceGrpc.HelloServiceBlockingStub;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethodWherePayloadContains;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnRequest;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FilibusterConditionalByEnvironmentSuite
public class JUnitFilibusterHelloUnimplementedTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestsExecuted = 0;

    private static int numberOfExceptionsThrown = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(searchStrategy= FilibusterSearchStrategy.BFS, maxIterations=10)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        boolean expected = false;

        numberOfTestsExecuted++;

        HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        HelloRequest request = HelloRequest.newBuilder().setName("Armerian").build();

        try {
            HelloReply reply = blockingStub.unimplemented(request);

            // Should never reach these assertions.
            assertEquals("Hello, Armerian World!!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            numberOfExceptionsThrown++;
            testExceptionsThrown.add(t.getMessage());

            boolean wasFaultInjected = wasFaultInjected();

            if (wasFaultInjected) {
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

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
                    expected = true;
                }

                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("WorldService");
                assertTrue(wasFaultInjectedOnWorldService);

                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World");
                assertTrue(wasFaultInjectedOnWorldMethod);

                boolean wasFaultInjectedOnRequest = wasFaultInjectedOnRequest(request.toString());
                assertTrue(wasFaultInjectedOnRequest);

                boolean wasFaultInjectedOnWorldMethodWithPayload = wasFaultInjectedOnMethodWherePayloadContains("cloud.filibuster.examples.WorldService/World", request.toString());
                assertTrue(wasFaultInjectedOnWorldMethodWithPayload);

                if (!expected) {
                    throw t;
                }
            } else {
                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED: Method cloud.filibuster.examples.WorldService/WorldUnimplemented is unimplemented")) {
                    expected = true;
                }

                if (!expected) {
                    throw t;
                }
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct exceptions thrown.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(6, testExceptionsThrown.size());
    }

    @DisplayName("Verify correct number of executed tests.")
    @Test
    @Order(3)
    public void testNumberOfTestsExecuted() {
        assertEquals(6, numberOfTestsExecuted);
    }

    @DisplayName("Verify correct number of exceptions thrown.")
    @Test
    @Order(4)
    public void numberOfExceptionsThrown() {
        assertEquals(6, numberOfExceptionsThrown);
    }
}