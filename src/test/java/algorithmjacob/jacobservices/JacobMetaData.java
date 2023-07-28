package algorithmjacob.jacobservices;
import java.util.ArrayList;
import cloud.filibuster.examples.AppendString;
//import io.grpc.stub.StreamObserver;

public class JacobMetaData {
    public ArrayList<Float> usedCallIDs;
    public String retval;
    public Boolean Completed;
   // public StreamObserver<Jacobalg.AppendReply> responseObserver;
    public AppendString.AppendRequest req;
    public JacobMetaData(int listSize,  AppendString.AppendRequest myReq){ //will also eventually include return address
        usedCallIDs = new ArrayList<>(listSize);
        for (int i = 0; i < listSize; i++) {
            usedCallIDs.add(-1f);
        }
        //responseObserver = myResponseObserver;
        Completed = false;
        req = myReq;
    }
}
