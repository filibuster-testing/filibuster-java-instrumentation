package algorithmjacob.jacobservices;
import java.util.ArrayList;
import cloud.filibuster.examples.Jacobalg;
public class JacobMetaData {
    public ArrayList<Float> usedCallIDs;
    public String retval;
    public Boolean Completed;
    //public StreamObserver<Jacobalg.AppendReply> responseObserver;
    public Jacobalg.AppendRequest req;
    public JacobMetaData(int listSize,  Jacobalg.AppendRequest myReq){ //will also eventually include return address
        usedCallIDs = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            usedCallIDs.add(-1f);
        }
        //responseObserver = myResponseObserver;
        Completed = false;
        req = myReq;
    }
}
