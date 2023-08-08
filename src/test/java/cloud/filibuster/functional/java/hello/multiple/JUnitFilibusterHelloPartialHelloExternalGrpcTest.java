package cloud.filibuster.functional.java.hello.multiple;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
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

import static cloud.filibuster.junit.assertions.GenericAssertions.wasFaultInjected;
import static cloud.filibuster.junit.assertions.GrpcAssertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethodWherePayloadContains;
import static cloud.filibuster.junit.assertions.GrpcAssertions.wasFaultInjectedOnRequest;
import static cloud.filibuster.junit.assertions.GrpcAssertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterHelloPartialHelloExternalGrpcTest extends JUnitAnnotationBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestsExecuted = 0;

    private static int numberOfExceptionsThrown = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(maxIterations=30)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        boolean expected = false;

        numberOfTestsExecuted++;

        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();

        try {
            Hello.HelloReply reply = blockingStub.partialHelloExternalGrpc(request);
            assertEquals("Hello, Hello, Hello, Armerian!!", reply.getMessage());
            assertFalse(wasFaultInjected());
        } catch (Throwable t) {
            numberOfExceptionsThrown++;
            testExceptionsThrown.add(t.getMessage());

            boolean wasFaultInjected = wasFaultInjected();

            boolean firstRPCFailed = false;
            boolean secondRPCFailed = false;

            if (wasFaultInjected) {
                // First RPC failed.
                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                    firstRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                    firstRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                    firstRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                    firstRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
                    expected = true;
                    firstRPCFailed = true;
                }

                if (firstRPCFailed) {
                    boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("WorldService");
                    assertTrue(wasFaultInjectedOnWorldService);

                    boolean wasFaultInjectedOnRequestString = wasFaultInjectedOnRequest(request);
                    assertTrue(wasFaultInjectedOnRequestString);

                    boolean wasFaultInjectedOnRequest = wasFaultInjectedOnRequest(request);
                    assertTrue(wasFaultInjectedOnRequest);

                    boolean wasFaultInjectedOnWorldMethodWithPayload = wasFaultInjectedOnMethodWherePayloadContains("cloud.filibuster.examples.WorldService/World", request.toString());
                    assertTrue(wasFaultInjectedOnWorldMethodWithPayload);
                }

                // Second RPC failed.

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
                    expected = true;
                    secondRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
                    expected = true;
                    secondRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
                    expected = true;
                    secondRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
                    expected = true;
                    secondRPCFailed = true;
                }

                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DATA_LOSS: io.grpc.StatusRuntimeException: UNKNOWN")) {
                    expected = true;
                    secondRPCFailed = true;
                }

                if (secondRPCFailed) {
                    boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("HelloService");
                    assertTrue(wasFaultInjectedOnWorldService);

                    boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod(HelloServiceGrpc.getHelloMethod());
                    assertTrue(wasFaultInjectedOnWorldMethod);
                }

                if (!expected) {
                    throw t;
                }
            } else {
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
        assertEquals(10, testExceptionsThrown.size());
    }

    @DisplayName("Verify correct number of executed tests.")
    @Test
    @Order(3)
    public void testNumberOfTestsExecuted() {
        assertEquals(11, numberOfTestsExecuted);
    }

    @DisplayName("Verify correct number of exceptions thrown.")
    @Test
    @Order(4)
    public void numberOfExceptionsThrown() {
        assertEquals(10, numberOfExceptionsThrown);
    }
}