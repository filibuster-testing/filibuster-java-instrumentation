package cloud.filibuster.junit.configuration.examples.armeria.http;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.transformers.NullTransformer;

public class FilibusterHTTPNullTransformerAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // Armeria's WebClient exception types.
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.WebClient.exceptions")
                .pattern("WebClient\\.(GET|PUT|POST|HEAD)");

        filibusterAnalysisConfigurationBuilder.transformer(NullTransformer.class);

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilder.build());

        // Generate configuration file.
        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
