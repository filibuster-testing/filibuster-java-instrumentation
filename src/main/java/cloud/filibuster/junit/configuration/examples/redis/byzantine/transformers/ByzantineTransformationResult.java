package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import org.json.JSONObject;

public class ByzantineTransformationResult<T> {
    public T value;
    public JSONObject accumulator;
    public boolean hasNext = false;

    @Override
    public String toString() {
        JSONObject result = new JSONObject();
        result.put("value", value);
        result.put("accumulator", accumulator);
        result.put("hasNext", hasNext);
        return result.toString();
    }
}
