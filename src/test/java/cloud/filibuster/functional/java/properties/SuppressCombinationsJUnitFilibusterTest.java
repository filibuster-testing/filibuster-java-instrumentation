package cloud.filibuster.functional.java.properties;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterConditionalByEnvironmentSuite;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.instrumentation.helpers.Property.SUPPRESS_COMBINATIONS_DEFAULT;
import static cloud.filibuster.instrumentation.helpers.Property.setTestSuppressCombinationsProperty;
import static cloud.filibuster.junit.Assertions.assertPassesAndThrowsOnlyUnderFault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@FilibusterConditionalByEnvironmentSuite
public class SuppressCombinationsJUnitFilibusterTest extends JUnitAnnotationBaseTest {

    @BeforeAll
    public static void setSuppressCombinationsProperty() {
        setTestSuppressCombinationsProperty(true);
    }

    @AfterAll
    public static void resetSuppressCombinationsProperty() {
        setTestSuppressCombinationsProperty(SUPPRESS_COMBINATIONS_DEFAULT);
    }

    private final static Set<String> testExceptionsThrown = new HashSet<>();

    private static int numberOfTestExecutions = 0;

    private final List<String> possibleResponses = Arrays.asList(
            "Hello, Armerian World!! Hello, Parallel World!!",
            "Hello, Parallel World!!",
            "Hello, Armerian World!!"
    );

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster()
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws Throwable {
        numberOfTestExecutions++;

        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

        assertPassesAndThrowsOnlyUnderFault(() -> {
            HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
            Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
            Hello.HelloReply reply = blockingStub.parallelPartialHello(request);
            assertTrue(possibleResponses.contains(reply.getMessage()));
        }, (t) -> {
            testExceptionsThrown.add(t.getMessage());
            throw t;
        });

        helloChannel.shutdownNow();
        helloChannel.awaitTermination(1000, TimeUnit.SECONDS);
    }

    @DisplayName("Verify correct number of test executions.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        assertEquals(11, numberOfTestExecutions);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumExceptions() {
        assertEquals(0, testExceptionsThrown.size());
    }
}