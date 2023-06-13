package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Random;

public class ByzantineStringTransformer implements ByzantineTransformer<String> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;
    private JSONObject accumulator;

    @Override
    public String transform(String payload, @Nonnull JSONObject accumulator) {
        int idx = extractIdxAndBuildAccumulator(payload, accumulator);

        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, generateRandomChar());

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

    private static char generateRandomChar() {
        // ASCII printable characters range from 33 to 126. Upper bound in nextInt is exclusive, hence 127.
        return (char) (rand.nextInt(127) + 33);
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
            idx = accumulator.getInt("idx") + 1;  // Move to the next idx
        }
        this.accumulator.put("idx", idx);
        return idx;
    }

}
