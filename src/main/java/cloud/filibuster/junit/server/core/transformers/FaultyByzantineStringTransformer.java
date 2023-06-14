package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class FaultyByzantineStringTransformer implements ByzantineTransformer<String> {
    private boolean hasNext = true;
    private JSONObject accumulator;

    @Override
    public String transform(String payload, @Nonnull JSONObject accumulator) {
        int idx = extractIdxAndBuildAccumulator(payload, accumulator);

        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, 'X'); // Fault: Always insert the same char

        if (idx == payload.length() - 1) {
            this.hasNext = false;
        }

        return newString.toString();
    }

    @Override
    public JSONObject getNewAccumulator() {
        if (accumulator == null) {
            throw new FilibusterRuntimeException("Accumulator is null. " +
                    "Please call the transform method first.");
        }
        return accumulator;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    private int extractIdxAndBuildAccumulator(String payload, JSONObject accumulator) {
        this.accumulator = accumulator;
        int idx;
        if (!accumulator.has("idx")) {
            // If accumulator does not have an idx, it indicates that this is the first time the transform method is called.
            // Hence, we set idx to 0. Payload is the original value from the reference execution.
            idx = 0;
            this.accumulator.put("originalValue", payload);
        } else {
            idx = accumulator.getInt("idx");  // Fault: Do not increment idx
        }
        this.accumulator.put("idx", idx);
        return idx;
    }

}
