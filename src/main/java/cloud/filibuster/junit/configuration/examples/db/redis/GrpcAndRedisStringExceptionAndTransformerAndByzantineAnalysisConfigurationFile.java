package cloud.filibuster.junit.configuration.examples.db.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.examples.db.byzantine.types.ByzantineStringFaultType;
import cloud.filibuster.junit.server.core.transformers.StringTransformer;

import java.util.HashMap;
import java.util.Map;

public class GrpcAndRedisStringExceptionAndTransformerAndByzantineAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createErrorMap(String cause) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("code", "");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();


        // Redis timeout exception for string get
        String[][] exceptions = new String[][]{
                {"io.lettuce.core.RedisCommandTimeoutException",
                        "(io.lettuce.core.api.sync.RedisStringCommands/get|" +
                                "io.lettuce.core.api.sync.RedisStringCommands/set)\\b",
                        "Command timed out after 100 millisecond(s)"},
        };

        for (String[] exception : exceptions) {
            createException(filibusterCustomAnalysisConfigurationFileBuilder, exception[0], exception[1], exception[2]);
        }


        // Transformer faults
        FilibusterAnalysisConfiguration.Builder transformerAnyByzantineConfigBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.transformers.transform_string.redis")
                .pattern("io.lettuce.core.api.sync.RedisStringCommands/get\\b");
        transformerAnyByzantineConfigBuilder.transformer(StringTransformer.class);


        // Byzantine faults
        String[] possibleValues = {null};
        for (String value : possibleValues) {
            // Communicate the byzantine fault type and value to the analysis configuration builder
            transformerAnyByzantineConfigBuilder.byzantine(new ByzantineStringFaultType(), value);
        }

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(transformerAnyByzantineConfigBuilder.build());


        // GRPC exception
        FilibusterAnalysisConfiguration.Builder grpcConfigBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)")
                .type("grpc");

        Map<String, String> grpcMap = new HashMap<>();
        grpcMap.put("cause", "");
        grpcMap.put("code", "UNAVAILABLE");

        grpcConfigBuilder.exception("io.grpc.StatusRuntimeException", grpcMap);

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(grpcConfigBuilder.build());


        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    private static void createException(FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder, String name, String pattern, String cause) {

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder().name(name).pattern(pattern);

        filibusterAnalysisConfigurationBuilderRedisExceptions.exception(name, createErrorMap(cause));

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
