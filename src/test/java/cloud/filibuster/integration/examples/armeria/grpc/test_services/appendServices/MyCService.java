package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.CGrpc;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;



public class MyCService extends CGrpc.CImplBase {
    private static MetaDataContainer metadataContainer;
    public static int cExecutionCounter = 0;
    public static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/CMetaData.json";

    public MyCService() {
        MyCServiceResetFunction();
    }

    public static void MyCServiceResetFunction(){
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

    public static void clearRedis() {
        RedisClientService.getInstance().redisClient.connect().sync().del(metadataPath);
    }

    @Override
    public void appendC(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {
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
            cExecutionCounter++;
            reply = AppendString.AppendReply.newBuilder()
                    .setReply(req.getBase() + "C")
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