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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MyAService extends AGrpc.AImplBase{

    public static boolean appendFinished = false;
    private static final Logger logger = Logger.getLogger(MyAService.class.getName());

    public static ManagedChannel BChannel;
    public static int aExecutionCounter = 0;
    private static Lock lock = new ReentrantLock();
    private static Condition observedCondition = lock.newCondition();

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
            /*Thread callThread = new Thread(()-> {
                    try {
                        reply.set(callB(newID, req, responseObserver));
                    } finally {

                    }
            });

            Thread observeThread = new Thread(() -> {
                    try {
                        checkConditional();
                    } finally {

                    }

            });

            callThread.start();
            observeThread.start();*/

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
        BChannel = ManagedChannelBuilder
                .forAddress(Networking.getHost("B"), Networking.getPort("B"))
                .usePlaintext()
                .build();
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

    public void checkConditional(){

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