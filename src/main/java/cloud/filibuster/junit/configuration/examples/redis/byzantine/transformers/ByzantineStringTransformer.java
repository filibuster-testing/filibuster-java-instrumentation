package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import org.json.JSONObject;

import java.util.Random;

public class ByzantineStringTransformer implements ByzantineTransformer<String, JSONObject> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;

    @Override
    public String transform(String payload, JSONObject accumulator) {
        if (!accumulator.has("idx")) {
            throw new FilibusterFaultInjectionException("String accumulator does not have idx key");
        }
        int idx = Integer.parseInt(accumulator.getString("idx"));

        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, generateRandomChar());

        if (idx == payload.length() - 1) {
            hasNext = false;
        }

        return newString.toString();
    }

    private static char generateRandomChar() {
        // ASCII printable characters range from 33 to 126. Upper bound in nextInt is exclusive, hence 127.
        return (char) (rand.nextInt(127) + 33);
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }
}
