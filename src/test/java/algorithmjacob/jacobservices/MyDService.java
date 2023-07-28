package algorithmjacob.jacobservices;
import cloud.filibuster.examples.DGrpc;
import cloud.filibuster.examples.AppendString;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;


public class MyDService extends DGrpc.DImplBase {
    private final MetaDataContainer metadataContainer;
    private static final String metadataPath = (new File("").getAbsolutePath()) + "/src/test/java/algorithmjacob/jacobservices/DMetaData.json";

    public MyDService() {
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
    public void appendD(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {
        AppendString.AppendReply reply;  // = AppendString.AppendReply.newBuilder().build();
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

 /*   public Float generateNewID(MetaDataContainer existingData){
        List<Float> existingIDs = existingData.getGeneratedIDs();
        Float newID;

        if (existingIDs.isEmpty()) {
            newID = 0.8f;
        } else {
            newID = existingIDs.get(existingIDs.size() - 1) + 1;
        }

        existingIDs.add(newID);
        existingData.setGeneratedIDs(existingIDs);
        JsonUtil.writeMetaData(existingData, metadataPath);
        return newID;
    }*/

}