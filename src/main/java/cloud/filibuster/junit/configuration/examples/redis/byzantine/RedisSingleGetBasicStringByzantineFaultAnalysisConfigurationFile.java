package cloud.filibuster.junit.configuration.examples.redis.byzantine;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.examples.redis.byzantine.types.ByzantineStringFaultType;

import java.util.HashMap;
import java.util.Map;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;

public class RedisSingleGetBasicStringByzantineFaultAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static <T> Map<String, T> createBzyantineFaultMap(T value) {
        Map<String, T> myMap = new HashMap<>();
        myMap.put("value", value);
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.lettuce.byzantine.basic_string")
                .pattern(REDIS_MODULE_NAME + "/(io.lettuce.core.api.sync.RedisStringCommands.get)\\b");


        String[] possibleValues = {null, ""};
        for (String value : possibleValues) {
            filibusterAnalysisConfigurationBuilderRedisExceptions.byzantine(new ByzantineStringFaultType(), createBzyantineFaultMap(value));
        }

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
