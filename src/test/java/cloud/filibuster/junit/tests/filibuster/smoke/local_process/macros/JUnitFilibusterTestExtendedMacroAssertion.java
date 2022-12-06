package cloud.filibuster.junit.tests.filibuster.smoke.local_process.macros;

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
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.TimeUnit;

import static cloud.filibuster.junit.Assertions.assertPassesOrThrowsUnderFault;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnMethod;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTestExtendedMacroAssertion extends JUnitBaseTest {

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @FilibusterTest(serverBackend=FilibusterLocalProcessServerBackend.class)
    public void testMyHelloAndMyWorldServiceWithFilibusterWithMacro() throws Throwable {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        assertPassesOrThrowsUnderFault(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        }, (t) -> {
            boolean expected = false;

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

            boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World");
            assertTrue(wasFaultInjectedOnWorldMethod);

            if (! expected) {
                throw new AssertionFailedError("Received unexpected exception.", t);
            }
        });

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @FilibusterTest(serverBackend=FilibusterLocalProcessServerBackend.class, expected = StatusRuntimeException.class)
    public void testMyHelloAndMyWorldServiceWithFilibusterWithMacroAndFailure() throws Throwable {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), 8765)
                .usePlaintext()
                .build();

        assertPassesOrThrowsUnderFault(StatusRuntimeException.class, () -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        }, (t) -> {
            assertEquals(true, false); // Should never get here since we never get past the initial execution.
        });

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

}