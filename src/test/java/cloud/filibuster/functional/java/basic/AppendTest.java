package cloud.filibuster.functional.java.basic;
import cloud.filibuster.examples.AGrpc;
import cloud.filibuster.examples.AppendString;
import cloud.filibuster.functional.java.JUnitAnnotationBaseTest;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices.*;
import cloud.filibuster.junit.TestWithFilibuster;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import static cloud.filibuster.integration.instrumentation.libraries.AppendTestHelper.startAppendServerAndWaitUntilAvailable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppendTest extends JUnitAnnotationBaseTest{

    private static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/AppendTestMetaData.json";
    private final MetaDataContainer metadataContainer;

    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAppendServerAndWaitUntilAvailable("A");
        startAppendServerAndWaitUntilAvailable("B");
        startAppendServerAndWaitUntilAvailable("C");
        startAppendServerAndWaitUntilAvailable("D");
    }

    public AppendTest() {
        MetaDataContainer existingData = JsonUtil.readMetaData(metadataPath);
        if (existingData != null) {
            this.metadataContainer = existingData;
        } else {
            this.metadataContainer = new MetaDataContainer();
            this.metadataContainer.setMetaDataMap(new HashMap<>());
            this.metadataContainer.setGeneratedIDs(new ArrayList<>());
            JsonUtil.writeMetaData(this.metadataContainer, metadataPath);
        }
    }


    @TestWithFilibuster()
    @Order(1)
    public void testABCDService() throws InterruptedException {

        ManagedChannel AChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("A"), Networking.getPort("A"))
                .usePlaintext()
                .build();
        float callID = 0.12341f;
        AGrpc.ABlockingStub blockingStub = AGrpc.newBlockingStub(AChannel);

        AppendString.AppendRequest request = AppendString.AppendRequest.newBuilder().setBase("Start").setCallID(callID).build();
        AppendString.AppendReply reply = blockingStub.appendA(request);
        assertEquals(reply.getReply(), ("StartDCBA"));
        if(metadataContainer.getMetaDataMap().containsKey(callID)){
            assertEquals(MyAService.aExecutionCounter, 0);
            assertEquals(MyBService.bExecutionCounter, 0);
            assertEquals(MyCService.cExecutionCounter, 0);
            assertEquals(MyDService.dExecutionCounter, 0);
            AChannel.shutdownNow();
            AChannel.awaitTermination(1000, TimeUnit.SECONDS);
            }
        else {
            JacobMetaData jacobMetaData = new JacobMetaData(1, null);
            jacobMetaData.retval = reply.getReply();
            metadataContainer.getMetaDataMap().put(callID, jacobMetaData);
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            assertEquals(MyAService.aExecutionCounter, 1);
            assertEquals(MyBService.bExecutionCounter, 1);
            assertEquals(MyCService.cExecutionCounter, 1);
            assertEquals(MyDService.dExecutionCounter, 1);
            AChannel.shutdownNow();
            AChannel.awaitTermination(1000, TimeUnit.SECONDS);
        }
    }

}
