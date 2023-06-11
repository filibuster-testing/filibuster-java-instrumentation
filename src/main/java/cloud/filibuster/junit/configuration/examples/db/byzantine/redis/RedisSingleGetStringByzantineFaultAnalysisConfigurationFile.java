package cloud.filibuster.junit.configuration.examples.db.byzantine.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.examples.db.byzantine.types.ByzantineStringFaultType;

public class RedisSingleGetStringByzantineFaultAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.lettuce.byzantine.string")
                .pattern("io.lettuce.core.api.sync.RedisStringCommands/get\\b");


        // Potentially use junit-quickcheck to generate the possible values -> Would make the tests more "flaky"
        String[] possibleValues = {null, "123", "", "abcd", "-123ABC", "ThisIsATestString"};
        for (String value : possibleValues) {
            filibusterAnalysisConfigurationBuilderRedisExceptions.byzantine(new ByzantineStringFaultType(), value);
        }

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
