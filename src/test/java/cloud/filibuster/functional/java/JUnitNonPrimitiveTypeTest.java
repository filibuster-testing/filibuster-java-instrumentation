package cloud.filibuster.functional.java;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.Map;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitNonPrimitiveTypeTest extends JUnitAnnotationBaseTest {

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @DisplayName("Test partial hello server grpc route with Filibuster. (MyHelloService, MyWorldService)")
    @TestWithFilibuster(maxIterations = 1)
    @Order(1)
    public void testMyHelloAndMyWorldServiceWithFilibuster() throws InterruptedException {
        Map<String, String> myValues = Map.of("hello", "world", "foo", "bar");

        ManagedChannel originalChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("api_server"), Networking.getPort("api_server"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("api_server");
        Channel interceptedChannel = ClientInterceptors.intercept(originalChannel, clientInterceptor);

        APIServiceGrpc.APIServiceBlockingStub blockingStub = APIServiceGrpc.newBlockingStub(interceptedChannel);
        Hello.NonPrimitiveRequest request = Hello.NonPrimitiveRequest.newBuilder().putAllObj(myValues).build();

        // Call the API server
        // The called method attaches " - modified" to all values in the map
        Hello.NonPrimitiveResponse reply = blockingStub.nonPrimitive(request);

        // Assert that " - modified" was attached to all values in the map
        reply.getObjMap().forEach((key, value) ->
                assertEquals(myValues.get(key) + " - modified", value));
    }
}