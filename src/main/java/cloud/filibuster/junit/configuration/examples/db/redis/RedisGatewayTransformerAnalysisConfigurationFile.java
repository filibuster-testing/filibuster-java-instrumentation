package cloud.filibuster.junit.configuration.examples.db.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.transformers.GatewayTransformer;

public class RedisGatewayTransformerAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.transformers.gateway_transformer.redis")
                .pattern("io.lettuce.core.api.sync.RedisStringCommands/get\\b");

        filibusterAnalysisConfigurationBuilderRedisExceptions.transformer(GatewayTransformer.class);

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
