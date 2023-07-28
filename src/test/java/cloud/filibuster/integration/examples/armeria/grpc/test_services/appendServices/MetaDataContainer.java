package cloud.filibuster.integration.examples.armeria.grpc.test_services.appendServices;

import java.util.HashMap;
import java.util.List;

public class MetaDataContainer {
    private HashMap<Float, JacobMetaData> metaDataMap;
    private List<Float> generatedIDs;

    public HashMap<Float, JacobMetaData> getMetaDataMap() {
        return metaDataMap;
    }

    public void setMetaDataMap(HashMap<Float, JacobMetaData> metaDataMap) {
        this.metaDataMap = metaDataMap;
    }

    public List<Float> getGeneratedIDs() {
        return generatedIDs;
    }

    public void setGeneratedIDs(List<Float> generatedIDs) {
        this.generatedIDs = generatedIDs;
    }
}
