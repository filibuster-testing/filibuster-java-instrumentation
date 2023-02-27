package cloud.filibuster.functional.java.smoke.compositional;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.JUnitBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.server.backends.FilibusterLocalServerBackend;
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
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test simple annotation usage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterHelloTest extends JUnitBaseTest {
    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private final static String name = "Armerian";

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(serverBackend=FilibusterLocalServerBackend.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        boolean expected = false;

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloExtendedRequest request = Hello.HelloExtendedRequest.newBuilder().setName(name).build();
            Hello.HelloExtendedReply reply = blockingStub.compositionalHello(request);
            assertEquals(name, reply.getName());
            assertEquals("Hello, " + name + " World!!", reply.getFirstMessage());
            assertThat(reply.getSecondMessage(), notNullValue());
            assertThat(reply.getCreatedAt(), notNullValue());
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

                boolean wasFaultInjectedOnWorldService = wasFaultInjectedOnService("WorldService");
                assertTrue(wasFaultInjectedOnWorldService);

                boolean wasFaultInjectedOnWorldMethod = wasFaultInjectedOnMethod("cloud.filibuster.examples.WorldService/World");
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
        assertEqualsUnlessFilibusterDisabledByEnvironment(4, testExceptionsThrown.size());
    }
}