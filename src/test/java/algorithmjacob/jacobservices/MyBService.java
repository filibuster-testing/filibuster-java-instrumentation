package algorithmjacob.jacobservices;

import cloud.filibuster.examples.BGrpc;
import cloud.filibuster.examples.CGrpc;
import cloud.filibuster.examples.DGrpc;
import cloud.filibuster.examples.Jacobalg;
import cloud.filibuster.instrumentation.helpers.Networking;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


public class MyBService extends BGrpc.BImplBase {

    private final MetaDataContainer metadataContainer;
    private static final String metadataPath = "/home/jwetzel/filibuster-java-instrumentation/src/test/java/algorithmjacob/jacobservices/BMetaData.json";

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
        /*for (HashMap.Entry<Float, JacobMetaData> entry : metadataContainer.getMetaDataMap().entrySet()) {
            if (entry.getValue().Completed = false) {
                appendB(entry.getValue().req, entry.getValue().responseObserver);
            }
        }*/
    }

    //private static final Logger logger = Logger.getLogger(MyBService.class.getName());
    @Override
    public void appendB(Jacobalg.AppendRequest req, StreamObserver<Jacobalg.AppendReply> responseObserver) {
        Jacobalg.AppendReply reply = Jacobalg.AppendReply.newBuilder().setReplyString(req.getBaseString()).build();
        if(metadataContainer.getMetaDataMap().containsKey(req.getCallID())) {
            JacobMetaData existingMetaData = metadataContainer.getMetaDataMap().get(req.getCallID());
            if (existingMetaData.retval != null) {
                reply = Jacobalg.AppendReply.newBuilder()
                        .setReplyString(existingMetaData.retval)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
                return;
            }
            if(existingMetaData.usedCallIDs.get(0) != -1f){
                callD(existingMetaData.usedCallIDs.get(0), reply);
            }else{
                Float newID = generateNewID(metadataContainer);
                existingMetaData.usedCallIDs.set(0, newID);
                reply = callD(newID, reply);
            }
            if(existingMetaData.usedCallIDs.get(1) != -1f){
                reply = callC(existingMetaData.usedCallIDs.get(1), reply);
            }else{
                Float newID = generateNewID(metadataContainer);
                existingMetaData.usedCallIDs.set(1, newID);
                reply = callC(newID, reply);
            }
        }
        else{
            JacobMetaData newMetaData = new JacobMetaData(2, req);
            metadataContainer.getMetaDataMap().put(req.getCallID(), newMetaData);

            Float newIDD = generateNewID(metadataContainer);
            newMetaData.usedCallIDs.set(0, newIDD);
            metadataContainer.setGeneratedIDs(metadataContainer.getGeneratedIDs());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            reply = callD(newIDD, reply);
            Float newIDC = generateNewID(metadataContainer);
            newMetaData.usedCallIDs.set(1, newIDC);
            metadataContainer.setGeneratedIDs(metadataContainer.getGeneratedIDs());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
            reply = callC(newIDC, reply);

        }
        reply = Jacobalg.AppendReply.newBuilder()
                .setReplyString(reply.getReplyString() + "B")
                .build();
        JacobMetaData metaData = metadataContainer.getMetaDataMap().get(req.getCallID());
        metaData.retval = reply.getReplyString();
        metaData.Completed = true;
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @SuppressWarnings("UnnecessaryBoxedVariable")
    private Jacobalg.AppendReply callC(Float callID, Jacobalg.AppendReply reply){
        ManagedChannel CChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("C"), Networking.getPort("C"))
                .usePlaintext()
                .build();
        CGrpc.CBlockingStub blockingStubC = CGrpc.newBlockingStub(CChannel);
        Jacobalg.AppendRequest Crequest = Jacobalg.AppendRequest.newBuilder().setBaseString(reply.getReplyString()).setCallID(callID).build();
        reply = Jacobalg.AppendReply.newBuilder().setReplyString(blockingStubC.appendC(Crequest).getReplyString()).build();
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        CChannel.shutdownNow();

        return reply;
    }
    @SuppressWarnings("UnnecessaryBoxedVariable")
    private Jacobalg.AppendReply callD(Float callID, Jacobalg.AppendReply reply){
        ManagedChannel DChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("D"), Networking.getPort("D"))
                .usePlaintext()
                .build();
        DGrpc.DBlockingStub blockingStubD = DGrpc.newBlockingStub(DChannel);
        Jacobalg.AppendRequest Drequest = Jacobalg.AppendRequest.newBuilder().setBaseString(reply.getReplyString()).setCallID(callID).build();
        reply = Jacobalg.AppendReply.newBuilder().setReplyString(blockingStubD.appendD(Drequest).getReplyString()).build();
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        DChannel.shutdownNow();
        return reply;
    }

    public Float generateNewID(MetaDataContainer existingData){
        List<Float> existingIDs = existingData.getGeneratedIDs();
        Float newID;

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