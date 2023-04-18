package cloud.filibuster.junit.formatters;

import static cloud.filibuster.junit.TestWithFilibuster.CURRENT_ITERATION_PLACEHOLDER;
import static cloud.filibuster.junit.TestWithFilibuster.DISPLAY_NAME_PLACEHOLDER;
import static cloud.filibuster.junit.TestWithFilibuster.TOTAL_ITERATIONS_PLACEHOLDER;

@SuppressWarnings("JavaDoc")
public class FilibusterTestDisplayNameFormatter {
    private final String firstPattern;
    private final String standardPattern;
    private final String displayName;

    @SuppressWarnings("JavaDoc")
    public FilibusterTestDisplayNameFormatter(String firstPattern, String standardPattern, String displayName) {
        this.firstPattern = firstPattern;
        this.standardPattern = standardPattern;
        this.displayName = displayName;
    }

    @SuppressWarnings("JavaDoc")
    public String format(int currentRepetition, int totalRepetitions) {
        if (currentRepetition == 1) {
            return this.firstPattern
                    .replace(DISPLAY_NAME_PLACEHOLDER, this.displayName)
                    .replace(CURRENT_ITERATION_PLACEHOLDER, String.valueOf(currentRepetition))
                    .replace(TOTAL_ITERATIONS_PLACEHOLDER, String.valueOf(totalRepetitions));
        } else {
            return this.standardPattern
                    .replace(DISPLAY_NAME_PLACEHOLDER, this.displayName)
                    .replace(CURRENT_ITERATION_PLACEHOLDER, String.valueOf(currentRepetition))
                    .replace(TOTAL_ITERATIONS_PLACEHOLDER, String.valueOf(totalRepetitions));
        }
    }
}
