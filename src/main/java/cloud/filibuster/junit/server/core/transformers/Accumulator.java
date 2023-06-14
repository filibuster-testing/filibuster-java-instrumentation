package cloud.filibuster.junit.server.core.transformers;

import java.io.Serializable;

public class Accumulator<PAYLOAD, CONTEXT> implements Serializable {
    private PAYLOAD referenceValue;
    private CONTEXT context;

    public CONTEXT getContext() {
        return this.context;
    }

    public void setContext(CONTEXT context) {
        this.context = context;
    }

    public PAYLOAD getReferenceValue() {
        return referenceValue;
    }

    public void setReferenceValue(PAYLOAD referenceValue) {
        this.referenceValue = referenceValue;
    }

}
