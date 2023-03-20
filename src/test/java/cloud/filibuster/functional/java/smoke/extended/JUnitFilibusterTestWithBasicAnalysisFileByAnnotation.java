package cloud.filibuster.functional.java.smoke.extended;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.server.backends.FilibusterLocalServerBackend;
import cloud.filibuster.functional.JUnitBaseTest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FilibusterConditionalByEnvironmentSuite
@SuppressWarnings("Java8ApiChecker")
public class JUnitFilibusterTestWithBasicAnalysisFileByAnnotation extends JUnitBaseTest {
    private static final List<String> basicGrpcErrorCodeList = new ArrayList<>();

    static {
        basicGrpcErrorCodeList.add("DEADLINE_EXCEEDED");
        basicGrpcErrorCodeList.add("UNAVAILABLE");
        basicGrpcErrorCodeList.add("INTERNAL");
        basicGrpcErrorCodeList.add("UNIMPLEMENTED");
    }

    private final static Set<String> testExceptionsThrown = new HashSet<>();

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(serverBackend=FilibusterLocalServerBackend.class)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        try {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.partialHello(request);
            assertEquals("Hello, Armerian World!!", reply.getMessage());
        } catch (Throwable t) {
            if (wasFaultInjected()) {
                testExceptionsThrown.add(t.getMessage());

                boolean found = false;

                for (String errorCode: basicGrpcErrorCodeList) {
                    String expectedString = "DATA_LOSS: io.grpc.StatusRuntimeException: " + errorCode;
                    if(t.getMessage().equals(expectedString)) {
                        found = true;
                    }
                }

                if (! found) {
                    throw t;
                }
            } else {
                throw t;
            }
        }

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(4, testExceptionsThrown.size());
    }
}