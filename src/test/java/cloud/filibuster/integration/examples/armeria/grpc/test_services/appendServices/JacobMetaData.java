package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import cloud.filibuster.examples.AppendString;

import java.util.ArrayList;

public class JacobMetaData {
    public ArrayList<Float> usedCallIDs;
    public String retval;
    public Boolean isCompleted;

    public AppendString.AppendRequest req;
    public JacobMetaData(int listSize,  AppendString.AppendRequest myReq){ 
        usedCallIDs = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            usedCallIDs.add(-1f);
        }
        isCompleted = false;
        req = myReq;
    }
}
