package cloud.filibuster.functional.java.basic.nofi;

import cloud.filibuster.examples.AGrpc;
import cloud.filibuster.examples.AppendString;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import static cloud.filibuster.integration.instrumentation.TestHelper.*;

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
        AppendString.AppendRequest request = AppendString.AppendRequest.newBuilder().setBase("Start").setCallID(0.06f).build();
        AppendString.AppendReply reply = blockingStub.appendA(request);
        String testString = reply.getReply();
        if(!testString.equals("StartDCBA")){
            return;
        }
        AChannel.shutdownNow();
        AChannel.awaitTermination(1000, TimeUnit.SECONDS);

    }

}
