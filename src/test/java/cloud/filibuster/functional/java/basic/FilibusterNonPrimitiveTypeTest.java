package cloud.filibuster.functional.java.basic;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@Disabled
public class FilibusterNonPrimitiveTypeTest extends JUnitAnnotationBaseTest {

    @BeforeAll
    public static void startAPIService() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAPIService() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    @DisplayName("Test serialization of non-primitive types")
    @TestWithFilibuster
    @Order(1)
    public void testNonPrimitiveSerialization() {
        try {
            Map<String, String> myValues = new HashMap<>();
            myValues.put("hello", "world");
            myValues.put("foo", "bar");

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
        } catch (StatusRuntimeException e) {
            // Do nothing
        }
    }
}