package algorithmjacob.jacobservices;
import java.util.ArrayList;
import cloud.filibuster.examples.AppendString;

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
