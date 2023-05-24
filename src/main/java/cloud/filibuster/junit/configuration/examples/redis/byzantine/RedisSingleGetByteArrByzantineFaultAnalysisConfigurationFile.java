package cloud.filibuster.junit.configuration.examples.redis.byzantine;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.examples.redis.byzantine.types.ByzantineByteArrayFaultType;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;

public class RedisSingleGetByteArrByzantineFaultAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static <T> Map<String, T> createBzyantineFaultMap(T value) {
        Map<String, T> myMap = new HashMap<>();
        myMap.put("value", value);
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.lettuce.byzantine.byte_arr")
                .pattern(REDIS_MODULE_NAME + "/(get)\\b");


        String[] possibleValues = {"", "ThisIsATestString", "abcd", "1234!!", "-11"};
        for (String value : possibleValues) {
            filibusterAnalysisConfigurationBuilderRedisExceptions.byzantine(new ByzantineByteArrayFaultType(), createBzyantineFaultMap(value.getBytes(Charset.defaultCharset())));
        }
        // Inject null fault
        filibusterAnalysisConfigurationBuilderRedisExceptions.byzantine(new ByzantineByteArrayFaultType(), createBzyantineFaultMap(null));

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
