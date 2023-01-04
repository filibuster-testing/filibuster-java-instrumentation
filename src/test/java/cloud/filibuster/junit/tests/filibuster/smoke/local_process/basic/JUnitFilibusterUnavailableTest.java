package cloud.filibuster.junit.tests.filibuster.smoke.local_process.basic;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import cloud.filibuster.junit.server.backends.FilibusterLocalProcessServerBackend;
import cloud.filibuster.junit.tests.filibuster.JUnitBaseTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.Matchers.containsString;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterUnavailableTest extends JUnitBaseTest {
    private static int numberOfTestsExceptionsThrownFaultsInjected = 0;

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @FilibusterTest(serverBackend=FilibusterLocalProcessServerBackend.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            blockingStub.unavailable(request);
            assertTrue(false);
        } catch (StatusRuntimeException e) {
            if (wasFaultInjected()) {
                numberOfTestsExceptionsThrownFaultsInjected++;
                assertThat(e.getMessage(), containsString("DATA_LOSS: io.grpc.StatusRuntimeException:"));
            } else {
                // Actual response when the remote service is online but unimplemented.
                assertEquals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED: Method cloud.filibuster.examples.WorldService/WorldUnavailable is unimplemented", e.getMessage());
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    /**
     * Verify that Filibuster generated the correct number of fault injections.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(4, numberOfTestsExceptionsThrownFaultsInjected);
    }








//    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
//    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
//    @FilibusterTest(serverBackend=FilibusterLocalProcessServerBackend.class)
//    @Order(1)
//    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
//        ManagedChannel helloChannel = ManagedChannelBuilder
//                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
//                .usePlaintext()
//                .build();
//
//        boolean expected = false;
//
//        try {
//            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
//            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
//            Hello.HelloReply reply = blockingStub.unavailable(request);
//            assertEquals("Hello, Armerian World!!", reply.getMessage());
//            assertFalse(wasFaultInjected());
//        } catch (Throwable t) {
//            boolean wasFaultInjected = wasFaultInjected();
//
//            if (wasFaultInjected) {
//                numberOfTestsExceptionsThrownFaultsInjected++;
//
//                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED")) {
//                    expected = true;
//                }
//
//                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNAVAILABLE")) {
//                    expected = true;
//                }
//
//                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: UNIMPLEMENTED")) {
//                    expected = true;
//                }
//
//                if (t.getMessage().equals("DATA_LOSS: io.grpc.StatusRuntimeException: INTERNAL")) {
//                    expected = true;
//                }
//
//                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("world");
//                assertTrue(wasFaultInjectedOnWorldService);
//
//                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World");
//                assertTrue(wasFaultInjectedOnWorldMethod);
//
//                if (! expected) {
//                    throw t;
//                }
//            } else {
//                throw t;
//            }
//        }
//
//        helloChannel.shutdownNow();
//        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
//    }
//
//    /**
//     * Verify that Filibuster generated the correct number of fault injections.
//     */
//    @DisplayName("Verify correct number of generated Filibuster tests.")
//    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
//    @Test
//    @Order(2)
//    public void testNumAssertions() {
//        assertEquals(4, numberOfTestsExceptionsThrownFaultsInjected);
//    }
}