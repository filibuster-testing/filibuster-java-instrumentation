package cloud.filibuster.junit.resolvers;

import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.ToStringBuilder;

@SuppressWarnings("JavaDoc")
public class FilibusterIterationInfoParameterResolver implements ParameterResolver {
    private final int currentIteration;
    private final int maxIterations;

    @SuppressWarnings("JavaDoc")
    public FilibusterIterationInfoParameterResolver(int currentIteration, int maxIterations) {
        this.currentIteration = currentIteration;
        this.maxIterations = maxIterations;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return (parameterContext.getParameter().getType() == RepetitionInfo.class);
    }

    @Override
    public RepetitionInfo resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return new DefaultRepetitionInfo(this.currentIteration, this.maxIterations);
    }

    private static class DefaultRepetitionInfo implements RepetitionInfo {
        private final int currentIteration;
        private final int maxIterations;

        DefaultRepetitionInfo(int currentIteration, int maxIterations) {
            this.currentIteration = currentIteration;
            this.maxIterations = maxIterations;
        }

        @Override
        public int getCurrentRepetition() {
            return this.currentIteration;
        }

        @Override
        public int getTotalRepetitions() {
            return this.maxIterations;
        }

        @Override
        public String toString() {
            // @formatter:off
            return new ToStringBuilder(this)
                    .append("currentIteration", this.currentIteration)
                    .append("maxIterations", this.maxIterations)
                    .toString();
            // @formatter:on
        }
    }
}