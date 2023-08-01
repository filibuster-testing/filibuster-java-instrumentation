package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.DGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class MyDService extends DGrpc.DImplBase {
    private static MetaDataContainer metadataContainer;
    public static int dExecutionCounter = 0;
    public static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/DMetaData.json";

    public MyDService() {
        MyDServiceResetFunction();
    }

    @BeforeEach
    public static void MyDServiceResetFunction(){
        MetaDataContainer existingData = JsonUtil.readMetaData(metadataPath);
        if (existingData != null) {
            metadataContainer = existingData;
        } else {
            metadataContainer = new MetaDataContainer();
            metadataContainer.setMetaDataMap(new HashMap<>());
            metadataContainer.setGeneratedIDs(new ArrayList<>());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
        }
    }
    @BeforeEach
    public void clearRedis() {
        RedisClientService.getInstance().redisClient.connect().sync().del(metadataPath);
    }

    @Override
    public void appendD(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {
        AppendString.AppendReply reply;
        if(metadataContainer.getMetaDataMap().containsKey(req.getCallID())) {
            JacobMetaData existingMetaData = metadataContainer.getMetaDataMap().get(req.getCallID());
            if (existingMetaData.retval != null) {
                reply = AppendString.AppendReply.newBuilder()
                        .setReply(existingMetaData.retval)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        }

        else{
            dExecutionCounter++;
            reply = AppendString.AppendReply.newBuilder()
                    .setReply(req.getBase() + "D")
                    .build();
            JacobMetaData newMetaData = new JacobMetaData(0, req);
            metadataContainer.getMetaDataMap().put(req.getCallID(), newMetaData);
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            JacobMetaData metaData = metadataContainer.getMetaDataMap().get(req.getCallID());
            metaData.retval = reply.getReply();
            metaData.isCompleted = true;
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

}