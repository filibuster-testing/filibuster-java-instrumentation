package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.BGrpc;
import cloud.filibuster.examples.CGrpc;
import cloud.filibuster.examples.DGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class MyBService extends BGrpc.BImplBase {

    private static MetaDataContainer metadataContainer;
    private static final Logger logger = Logger.getLogger(MyBService.class.getName());

    public static int bExecutionCounter = 0;
    public static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/BMetaData.json";
    public MyBService() {
        MyBServiceResetFunction();
    }

    public static void MyBServiceResetFunction(){
        MetaDataContainer existingData = JsonUtil.readMetaData(metadataPath);
        if (existingData != null) {
            metadataContainer = existingData;
        } else {
            metadataContainer = new MetaDataContainer();
            metadataContainer.setMetaDataMap(new HashMap<>());
            metadataContainer.setGeneratedIDs(new ArrayList<>());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
        }
        bExecutionCounter = 0;
    }

    public static void clearRedis() {
        RedisClientService.getInstance().redisClient.connect().sync().del(metadataPath);
    }

    @Override
    public void appendB(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {
        //MyAService.shutDownB();
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
        bExecutionCounter++;
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

    private AppendString.AppendReply callC(float callID, AppendString.AppendReply reply){
        ManagedChannel CManagedChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("C"), Networking.getPort("C"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("C");
        Channel CChannel = ClientInterceptors.intercept(CManagedChannel, clientInterceptor);
        CGrpc.CBlockingStub blockingStubC = CGrpc.newBlockingStub(CChannel);
        AppendString.AppendRequest Crequest = AppendString.AppendRequest.newBuilder().setBase(reply.getReply()).setCallID(callID).build();
        reply = AppendString.AppendReply.newBuilder().setReply("").build();

        while(!reply.getReply().equals("StartDC")){
            try{
                reply = blockingStubC.appendC(Crequest);
                break;
            } catch (Exception e) {
                logger.log(Level.WARNING, e.toString());
            }
        }

        CManagedChannel.shutdownNow();

        return reply;
    }

    private AppendString.AppendReply callD(float callID, AppendString.AppendReply reply){
        ManagedChannel DManagedChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("D"), Networking.getPort("D"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("D");
        Channel DChannel  = ClientInterceptors.intercept(DManagedChannel, clientInterceptor);
        DGrpc.DBlockingStub blockingStubD = DGrpc.newBlockingStub(DChannel);
        AppendString.AppendRequest Drequest = AppendString.AppendRequest.newBuilder().setBase(reply.getReply()).setCallID(callID).build();
        reply = AppendString.AppendReply.newBuilder().setReply("").build();

        while(!(reply.getReply().equals("StartD"))){
            try{
                reply = blockingStubD.appendD(Drequest);
                break;
            } catch (Exception e) {
                logger.log(Level.WARNING, e.toString());
            }
        }
        DManagedChannel.shutdownNow();
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