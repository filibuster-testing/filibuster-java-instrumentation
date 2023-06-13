package cloud.filibuster.junit.server.core.transformers;

import org.json.JSONObject;

public class ByzantineTransformationResult<T> {
    private T value;
    private JSONObject accumulator;
    private boolean hasNext = false;

    public JSONObject getAccumulator() {
        return accumulator;
    }

    public void setAccumulator(JSONObject accumulator) {
        this.accumulator = accumulator;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

}
