package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import org.json.JSONObject;

public class ByzantineTransformationResult<T> {
    public T value;
    public JSONObject accumulator;
    public boolean hasNext = false;
}
