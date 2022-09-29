package cloud.filibuster.junit.formatters;

import static cloud.filibuster.junit.FilibusterTest.CURRENT_ITERATION_PLACEHOLDER;
import static cloud.filibuster.junit.FilibusterTest.DISPLAY_NAME_PLACEHOLDER;
import static cloud.filibuster.junit.FilibusterTest.TOTAL_ITERATIONS_PLACEHOLDER;

@SuppressWarnings("JavaDoc")
public class FilibusterTestDisplayNameFormatter {
    private final String pattern;
    private final String displayName;

    @SuppressWarnings("JavaDoc")
    public FilibusterTestDisplayNameFormatter(String pattern, String displayName) {
        this.pattern = pattern;
        this.displayName = displayName;
    }

    @SuppressWarnings("JavaDoc")
    public String format(int currentRepetition, int totalRepetitions) {
        return this.pattern
                .replace(DISPLAY_NAME_PLACEHOLDER, this.displayName)
                .replace(CURRENT_ITERATION_PLACEHOLDER, String.valueOf(currentRepetition))
                .replace(TOTAL_ITERATIONS_PLACEHOLDER, String.valueOf(totalRepetitions));
    }
}
