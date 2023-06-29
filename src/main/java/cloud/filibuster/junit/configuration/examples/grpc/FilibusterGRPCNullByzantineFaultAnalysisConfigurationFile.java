package cloud.filibuster.junit.configuration.examples.grpc;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.transformers.NullTransformer;

public class FilibusterGRPCNullByzantineFaultAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.byzantine_fault")
                .pattern("(.*/.*)")
                .type("grpc");

        filibusterAnalysisConfigurationBuilder.transformer(NullTransformer.class);

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilder.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
