package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.BGrpc;
import cloud.filibuster.examples.CGrpc;
import cloud.filibuster.examples.DGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MyBService extends BGrpc.BImplBase {

    private final MetaDataContainer metadataContainer;
    private static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/BMetaData.json";
    public MyBService() {
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
    public void appendB(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {
        AppendString.AppendReply reply = AppendString.AppendReply.newBuilder().setReply(req.getBase()).build();
        if(metadataContainer.getMetaDataMap().containsKey(req.getCallID())) {
            JacobMetaData existingMetaData = metadataContainer.getMetaDataMap().get(req.getCallID());
            if (existingMetaData.retval != null) {
                reply = AppendString.AppendReply.newBuilder()
                        .setReply(existingMetaData.retval)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                return;
            }
            if(existingMetaData.usedCallIDs.get(0) != -1f){
                callD(existingMetaData.usedCallIDs.get(0), reply);
            }else{
                float newID = generateNewID(metadataContainer);
                existingMetaData.usedCallIDs.set(0, newID);
                reply = callD(newID, reply);
            }
            if(existingMetaData.usedCallIDs.get(1) != -1f){
                reply = callC(existingMetaData.usedCallIDs.get(1), reply);
            }else{
                float newID = generateNewID(metadataContainer);
                existingMetaData.usedCallIDs.set(1, newID);
                reply = callC(newID, reply);
            }
        }
        else{
            JacobMetaData newMetaData = new JacobMetaData(2, req);
            metadataContainer.getMetaDataMap().put(req.getCallID(), newMetaData);

            float newIDD = generateNewID(metadataContainer);
            newMetaData.usedCallIDs.set(0, newIDD);
            metadataContainer.setGeneratedIDs(metadataContainer.getGeneratedIDs());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            reply = callD(newIDD, reply);
            float newIDC = generateNewID(metadataContainer);
            newMetaData.usedCallIDs.set(1, newIDC);
            metadataContainer.setGeneratedIDs(metadataContainer.getGeneratedIDs());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            reply = callC(newIDC, reply);

        }
        reply = AppendString.AppendReply.newBuilder()
                .setReply(reply.getReply() + "B")
                .build();
        JacobMetaData metaData = metadataContainer.getMetaDataMap().get(req.getCallID());
        metaData.retval = reply.getReply();
        metaData.isCompleted = true;
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @SuppressWarnings("UnnecessaryBoxedVariable")
    private AppendString.AppendReply callC(float callID, AppendString.AppendReply reply){
        ManagedChannel CChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("C"), Networking.getPort("C"))
                .usePlaintext()
                .build();
        CGrpc.CBlockingStub blockingStubC = CGrpc.newBlockingStub(CChannel);
        AppendString.AppendRequest Crequest = AppendString.AppendRequest.newBuilder().setBase(reply.getReply()).setCallID(callID).build();
        reply = AppendString.AppendReply.newBuilder().setReply(blockingStubC.appendC(Crequest).getReply()).build();
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        CChannel.shutdownNow();

        return reply;
    }
    @SuppressWarnings("UnnecessaryBoxedVariable")
    private AppendString.AppendReply callD(float callID, AppendString.AppendReply reply){
        ManagedChannel DChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("D"), Networking.getPort("D"))
                .usePlaintext()
                .build();
        DGrpc.DBlockingStub blockingStubD = DGrpc.newBlockingStub(DChannel);
        AppendString.AppendRequest Drequest = AppendString.AppendRequest.newBuilder().setBase(reply.getReply()).setCallID(callID).build();
        reply = AppendString.AppendReply.newBuilder().setReply(blockingStubD.appendD(Drequest).getReply()).build();
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        DChannel.shutdownNow();
        return reply;
    }

    public float generateNewID(MetaDataContainer existingData){
        List<Float> existingIDs = existingData.getGeneratedIDs();
        float newID;

        if (existingIDs.isEmpty()) {
            newID = 0.6f;
        } else {
            newID = existingIDs.get(existingIDs.size() - 1) + 1;
        }

        existingIDs.add(newID);
        existingData.setGeneratedIDs(existingIDs);
        JsonUtil.writeMetaData(existingData, metadataPath);
        return newID;
    }

}