package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ByzantineStringTransformer implements ByzantineTransformer<String, Integer> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;

    @Override
    public String transform(String payload, Integer idx) {
        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, generateRandomChar());

        if (idx == payload.length() - 1) {
            hasNext = false;
        }

        return newString.toString();
    }

    public List<String> transformCompleteString(String refString) {
        List<String> transStrings = new ArrayList<>();
        // Traverse the string
        for (int i = 0; i < refString.length(); i++) {
            transStrings.add(transform(refString, i));
        }
        return transStrings;
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
