package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;
import cloud.filibuster.examples.AGrpc;
import cloud.filibuster.examples.AppendString;
import cloud.filibuster.examples.BGrpc;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterClientInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MyAService extends AGrpc.AImplBase{

    private static final Logger logger = Logger.getLogger(MyAService.class.getName());
    public static int aExecutionCounter = 0;

    private static MetaDataContainer metadataContainer = null;
    public static final String metadataPath = new File("").getAbsolutePath() + "/src/test/java/cloud/filibuster/integration/examples/armeria/grpc/test_services/appendServices/AMetaData.json";

    public MyAService() {
        MyAServiceResetFunction();
    }


    public static void MyAServiceResetFunction(){
        MetaDataContainer existingData = JsonUtil.readMetaData(metadataPath);
        if (existingData != null) {
            metadataContainer = existingData;
        } else {
            metadataContainer = new MetaDataContainer();
            metadataContainer.setMetaDataMap(new HashMap<>());
            metadataContainer.setGeneratedIDs(new ArrayList<>());
            JsonUtil.writeMetaData(metadataContainer, metadataPath);
        }
        aExecutionCounter = 0;
        for (HashMap.Entry<Float, JacobMetaData> entry : metadataContainer.getMetaDataMap().entrySet()) {
            if(!entry.getValue().isCompleted){
                ManagedChannel AChannel = ManagedChannelBuilder
                        .forAddress(Networking.getHost("A"), Networking.getPort("A"))
                        .usePlaintext()
                        .build();
                AGrpc.ABlockingStub blockingStub = AGrpc.newBlockingStub(AChannel);
                blockingStub.appendA(entry.getValue().req);
            }
        }
    }


    public static void clearRedis() {
        RedisClientService.getInstance().redisClient.connect().sync().del(metadataPath);
    }


    @Override
public void appendA(AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver) {

    AppendString.AppendReply reply = AppendString.AppendReply.newBuilder().setReply(req.getBase()).build();
    if(metadataContainer.getMetaDataMap().containsKey(req.getCallID())){
        JacobMetaData existingMetaData = metadataContainer.getMetaDataMap().get(req.getCallID());
        if(existingMetaData.retval != null){
            reply = (AppendString.AppendReply.newBuilder()
                    .setReply(existingMetaData.retval)
                    .build());
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            return;
        }

        if(existingMetaData.usedCallIDs.get(0) != -1f){
            callB(existingMetaData.usedCallIDs.get(0), req, responseObserver);
        }else{
            float newID = generateNewID(metadataContainer);
            existingMetaData.usedCallIDs.set(0, newID);
            reply = (callB(newID, req, responseObserver));

        }
    }else{
        JacobMetaData newMetaData = new JacobMetaData(1, req);
        float newID = generateNewID(metadataContainer);
        newMetaData.usedCallIDs.set(0, newID);
        metadataContainer.getMetaDataMap().put(req.getCallID(), newMetaData);
        metadataContainer.setGeneratedIDs(metadataContainer.getGeneratedIDs());
        JsonUtil.writeMetaData(metadataContainer, metadataPath);
        reply = (callB(newID, req, responseObserver));
    }

    aExecutionCounter++;
    reply = (AppendString.AppendReply.newBuilder()
            .setReply(reply.getReply()+ "A")
            .build());
    JacobMetaData metaData = metadataContainer.getMetaDataMap().get(req.getCallID());
    metaData.retval = reply.getReply();
    metaData.isCompleted = true;
    JsonUtil.writeMetaData(metadataContainer, metadataPath);
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
}

    public AppendString.AppendReply callB(float callID, AppendString.AppendRequest req, StreamObserver<AppendString.AppendReply> responseObserver){
        ManagedChannel BManagedChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("B"), Networking.getPort("B"))
                .usePlaintext()
                .build();
        ClientInterceptor clientInterceptor = new FilibusterClientInterceptor("A");
        Channel BChannel = ClientInterceptors.intercept(BManagedChannel, clientInterceptor);
        BGrpc.BBlockingStub blockingStubB = BGrpc.newBlockingStub(BChannel);

        AppendString.AppendRequest request = AppendString.AppendRequest.newBuilder()
                .setBase(req.getBase())
                .setCallID(callID)
                .build();

        AppendString.AppendReply reply = AppendString.AppendReply.newBuilder().setReply("").build();
        while(!reply.getReply().equals("StartDCB")){
            try{
                reply = blockingStubB.appendB(request);
                break;
            } catch (Exception e) {
                logger.log(Level.WARNING, e.toString());
            }
        }
        return reply;
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

}