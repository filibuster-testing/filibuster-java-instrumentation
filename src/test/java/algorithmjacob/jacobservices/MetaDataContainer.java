package algorithmjacob.jacobservices;

import java.util.HashMap;
import java.util.List;

public class MetaDataContainer {
    private HashMap<Float, JacobMetaData> metaDataMap;
    private List<Float> generatedIDs;

    // Getter and setter for metaDataMap
    public HashMap<Float, JacobMetaData> getMetaDataMap() {
        return metaDataMap;
    }

    public void setMetaDataMap(HashMap<Float, JacobMetaData> metaDataMap) {
        this.metaDataMap = metaDataMap;
    }

    // Getter and setter for generatedIDs
    public List<Float> getGeneratedIDs() {
        return generatedIDs;
    }

    public void setGeneratedIDs(List<Float> generatedIDs) {
        this.generatedIDs = generatedIDs;
    }
}
