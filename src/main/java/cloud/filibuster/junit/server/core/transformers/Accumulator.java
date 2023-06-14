package cloud.filibuster.junit.server.core.transformers;

import java.io.Serializable;

public class Accumulator<PAYLOAD, CONTEXT> implements Serializable {
    private PAYLOAD referenceValue;
    private CONTEXT counter;

    public CONTEXT getContext() {
        return this.counter;
    }

    public void setContext(CONTEXT CONTEXT) {
        this.counter = CONTEXT;
    }

    public PAYLOAD getReferenceValue() {
        return referenceValue;
    }
    public void setReferenceValue(PAYLOAD referenceValue) {
        this.referenceValue = referenceValue;
    }

}
