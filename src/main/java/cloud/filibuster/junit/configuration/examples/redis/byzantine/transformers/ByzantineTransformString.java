package cloud.filibuster.junit.configuration.examples.redis.byzantine.transformers;

import java.util.Random;

public class ByzantineTransformString implements ByzantineTransformer<String, Integer> {
    private static final long FIXED_SEED = 0;

    @Override
    public String transform(String payload, Integer idx) {
        StringBuilder newString = new StringBuilder(payload);
        newString.setCharAt(idx, generateRandomChar());

        return newString.toString();
    }

    private static char generateRandomChar() {
        Random rand = new Random(FIXED_SEED);
        // ASCII printable characters range from 33 to 126. Upper bound in nextInt is exclusive, hence 127.
        return (char) (rand.nextInt(127) + 33);
    }
}
