package cloud.filibuster.functional.java.basic.nofi;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.examples.HelloServiceGrpc;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JacobTest extends JUnitAnnotationBaseTest {
    @Test
    @Disabled
    public void testJacobTest()  {
        ManagedChannel helloChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("hello"), Networking.getPort("hello"))
                .usePlaintext()
                .build();

//        HelloServiceGrpc.HelloServiceBlockingStub blockingStub = HelloServiceGrpc.newBlockingStub(helloChannel);
//        Hello.HelloRequest request = Hello.HelloRequest.newBuilder().setName("Armerian").build();
//        Hello.HelloReply reply = blockingStub.jacobEndpoint(request);
//        assertEquals("Hello, Armerian World!! This is a new GRPC endpoint.", reply.getMessage());
    }
}