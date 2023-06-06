package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ByzantineTransformString implements ByzantineTransformer<String> {
    private static final long FIXED_SEED = 0;  // Seed is fixed to ensure consistent results
    public boolean hasNext = true;

    @Override
    public String transform(String payload, String accumulator, Integer idx) {
        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, generateRandomChar());

        if(idx == payload.length() - 1) {
            hasNext = false;
        }

        return newString.toString();
    }

    public String transformAtIdx(String refString, Integer idx) {
        return transform(refString, "", idx);
    }

    public List<String> transformCompleteString(String refString) {
        List<String> transStrings = new ArrayList<>();
        // Traverse the string
        for (int i = 0; i < refString.length(); i++) {
            transStrings.add(transformAtIdx(refString, i));
        }
        return transStrings;
    }

    private static char generateRandomChar() {
        Random rand = new Random(FIXED_SEED);
        // ASCII printable characters range from 33 to 126. Upper bound in nextInt is exclusive, hence 127.
        return (char) (rand.nextInt(127) + 33);
    }
}
