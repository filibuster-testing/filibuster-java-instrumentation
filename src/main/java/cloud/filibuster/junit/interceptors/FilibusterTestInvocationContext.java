package cloud.filibuster.junit.interceptors;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.resolvers.FilibusterIterationInfoParameterResolver;
import cloud.filibuster.junit.formatters.FilibusterTestDisplayNameFormatter;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("JavaDoc")
public class FilibusterTestInvocationContext implements TestTemplateInvocationContext {
    private final int currentIteration;
    private final int maxIterations;
    private final FilibusterTestDisplayNameFormatter formatter;
    private final FilibusterConfiguration filibusterConfiguration;
    private final HashMap<Integer, Boolean> invocationCompletionMap;

    @SuppressWarnings("JavaDoc")
    public FilibusterTestInvocationContext(
            int currentIteration,
            int maxIterations,
            FilibusterTestDisplayNameFormatter formatter,
            FilibusterConfiguration filibusterConfiguration,
            HashMap<Integer, Boolean> invocationCompletionMap) {
        this.currentIteration = currentIteration;
        this.maxIterations = maxIterations;
        this.formatter = formatter;
        this.filibusterConfiguration = filibusterConfiguration;
        this.invocationCompletionMap = invocationCompletionMap;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return this.formatter.format(this.currentIteration, this.maxIterations);
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        ArrayList<Extension> extensionList = new ArrayList<>();
        extensionList.add(new FilibusterIterationInfoParameterResolver(this.currentIteration, this.maxIterations));
        extensionList.add(new FilibusterInvocationInterceptor(this.filibusterConfiguration, this.currentIteration, this.maxIterations, this.invocationCompletionMap));
        return extensionList;
    }
}
