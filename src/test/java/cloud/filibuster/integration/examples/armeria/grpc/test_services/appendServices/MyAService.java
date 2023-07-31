package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AGrpc;
import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.BGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MyAService extends AGrpc.AImplBase{

    public static boolean appendFinished = false;
    public static ManagedChannel BChannel;
    public static int aExecutionCounter = 0;

    private final MetaDataContainer metadataContainer;
    private static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/AMetaData.json";
    public static boolean retryCall = false;

    public MyAService() {
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
public void appendA(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {

        //while(testTrue == false){

        //}

    if(metadataContainer.getMetaDataMap().containsKey(req.getCallID())){
        JacobMetaData existingMetaData = metadataContainer.getMetaDataMap().get(req.getCallID());
        if(existingMetaData.retval != null){
            AppendString.AppendReply reply = AppendString.AppendReply.newBuilder()
                    .setReply(existingMetaData.retval)
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            return;
        }

        if(existingMetaData.usedCallIDs.get(0) != -1f){
            callB(existingMetaData.usedCallIDs.get(0), req, responseObserver, existingMetaData);
        }else{
            float newID = generateNewID(metadataContainer);
            existingMetaData.usedCallIDs.set(0, newID);
            callB(newID, req, responseObserver, existingMetaData);
        }
    }else{
        JacobMetaData newMetaData = new JacobMetaData(1, req);
        float newID = generateNewID(metadataContainer);
        newMetaData.usedCallIDs.set(0, newID);
        metadataContainer.getMetaDataMap().put(req.getCallID(), newMetaData);
        metadataContainer.setGeneratedIDs(metadataContainer.getGeneratedIDs());
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        callB(newID, req, responseObserver, newMetaData);
    }
}

    public void callB(float callID, AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver, JacobMetaData metaData){
        BChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("B"), Networking.getPort("B"))
                .usePlaintext()
                .build();
        BGrpc.BBlockingStub blockingStubB = BGrpc.newBlockingStub(BChannel);

        AppendString.AppendRequest request = AppendString.AppendRequest.newBuilder()
                .setBase(req.getBase())
                .setCallID(callID)
                .build();

        /*try{
            thread
        }*/

        AppendString.AppendReply reply = blockingStubB.appendB(request);

        aExecutionCounter++;
        reply = AppendString.AppendReply.newBuilder()
                .setReply(reply.getReply() + "A")
                .build();
        metaData.retval = reply.getReply();
        metaData.isCompleted = true;
        JsonUtil.writeMetaData(metadataContainer, metadataPath);

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
    public float generateNewID(MetaDataContainer existingData){
        List<Float> existingIDs = existingData.getGeneratedIDs();
        float newID;

        if (existingIDs.isEmpty()) {
            newID = 0.5f;
        } else {
            newID = existingIDs.get(existingIDs.size() - 1) + 1;
        }

        existingIDs.add(newID);
        existingData.setGeneratedIDs(existingIDs);
        JsonUtil.writeMetaData(existingData, metadataPath);
        return newID;
    }

    public static void StatusUpdate(AppendString.RetryRequest req, StreamObserver<AppendString.AppendReply> responseObserver){

    }
}