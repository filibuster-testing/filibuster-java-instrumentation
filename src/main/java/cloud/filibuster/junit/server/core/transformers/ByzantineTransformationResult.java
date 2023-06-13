package cloud.filibuster.junit.server.core.transformers;

import org.json.JSONObject;

public class ByzantineTransformationResult<T> {
    public T value;
    public JSONObject accumulator;
    public boolean hasNext = false;
}
