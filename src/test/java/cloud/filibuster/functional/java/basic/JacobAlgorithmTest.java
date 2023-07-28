package cloud.filibuster.functional.java.basic;

import cloud.filibuster.examples.AGrpc;
import cloud.filibuster.examples.AppendString;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static cloud.filibuster.integration.instrumentation.TestHelper.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JacobAlgorithmTest extends JUnitAnnotationBaseTest{
    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAServerAndWaitUntilAvailable();
        startBServerAndWaitUntilAvailable();
        startCServerAndWaitUntilAvailable();
        startDServerAndWaitUntilAvailable();
    }



    @TestWithFilibuster()
    @Order(1)
    public void testABCDService() throws InterruptedException {

        ManagedChannel AChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("A"), Networking.getPort("A"))
                .usePlaintext()
                .build();

        AGrpc.ABlockingStub blockingStub = AGrpc.newBlockingStub(AChannel);
        AppendString.AppendRequest request = AppendString.AppendRequest.newBuilder().setBase("Start").setCallID(0.5f).build();
        AppendString.AppendReply reply = blockingStub.appendA(request);
        assertTrue(reply.getReply() == "StartDCBA");
        AChannel.shutdownNow();
        AChannel.awaitTermination(1000, TimeUnit.SECONDS);

    }

}
