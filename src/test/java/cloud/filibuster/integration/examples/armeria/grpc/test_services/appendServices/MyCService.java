package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.CGrpc;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;



public class MyCService extends CGrpc.CImplBase {
    private final MetaDataContainer metadataContainer;
    public static int cExecutionCounter = 0;
    private static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/CMetaData.json";

    public MyCService() {
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