package cloud.filibuster.instrumentation.datatypes;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Pairs.
 */
public class Pair {
    private Pair() {

    }

    /**
     * Generate a pair of two items; a tuple, if you will.
     *
     * @param first first item in the pair.
     * @param second second item in the pair.
     * @param <T> type of the first item.
     * @param <U> type of the second item.
     * @return a pair.
     */
    // Return a map entry (key-value pair) from the specified values
    public static <T, U> Map.Entry<T, U> of(T first, U second) {
        return new AbstractMap.SimpleEntry<>(first, second);
    }
}