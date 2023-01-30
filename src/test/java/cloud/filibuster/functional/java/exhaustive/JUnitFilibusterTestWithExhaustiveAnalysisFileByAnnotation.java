package cloud.filibuster.functional.java.exhaustive;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.configuration.FilibusterGrpcExhaustiveAnalysisConfigurationFile;
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
@SuppressWarnings("Java8ApiChecker")
public class JUnitFilibusterTestWithExhaustiveAnalysisFileByAnnotation extends JUnitBaseTest {
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();

    static {
        exhaustiveGrpcErrorCodeList.add("CANCELLED");
        exhaustiveGrpcErrorCodeList.add("UNKNOWN");
        exhaustiveGrpcErrorCodeList.add("INVALID_ARGUMENT");
        exhaustiveGrpcErrorCodeList.add("DEADLINE_EXCEEDED");
        exhaustiveGrpcErrorCodeList.add("NOT_FOUND");
        exhaustiveGrpcErrorCodeList.add("ALREADY_EXISTS");
        exhaustiveGrpcErrorCodeList.add("PERMISSION_DENIED");
        exhaustiveGrpcErrorCodeList.add("RESOURCE_EXHAUSTED");
        exhaustiveGrpcErrorCodeList.add("FAILED_PRECONDITION");
        exhaustiveGrpcErrorCodeList.add("ABORTED");
        exhaustiveGrpcErrorCodeList.add("OUT_OF_RANGE");
        exhaustiveGrpcErrorCodeList.add("UNIMPLEMENTED");
        exhaustiveGrpcErrorCodeList.add("INTERNAL");
        exhaustiveGrpcErrorCodeList.add("UNAVAILABLE");
        exhaustiveGrpcErrorCodeList.add("DATA_LOSS");
        exhaustiveGrpcErrorCodeList.add("UNAUTHENTICATED");
    }

    private final static Set<String> testExceptionsThrown = new HashSet<>();

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @FilibusterTest(analysisConfigurationFile=FilibusterGrpcExhaustiveAnalysisConfigurationFile.class, serverBackend=FilibusterLocalServerBackend.class)
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

                for (String errorCode: exhaustiveGrpcErrorCodeList) {
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
        assertEquals(16, testExceptionsThrown.size());
    }
}