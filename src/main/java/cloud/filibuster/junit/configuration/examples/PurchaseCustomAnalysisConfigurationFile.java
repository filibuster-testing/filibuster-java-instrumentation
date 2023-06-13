package cloud.filibuster.junit.configuration.examples;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PurchaseCustomAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createGrpcErrorMap(String code) {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", "");
        myMap.put("code", code);
        return myMap;
    }

    private static Map<String, String> createRedisErrorMap() {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", "Command timed out after 100 millisecond(s)");
        myMap.put("code", "");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // GRPC
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderGrpcExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.exceptions")
                .pattern("(.*/.*)")
                .type("grpc");
        filibusterAnalysisConfigurationBuilderGrpcExceptions.exception("io.grpc.StatusRuntimeException", createGrpcErrorMap("UNAVAILABLE"));
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderGrpcExceptions.build());

        // Redis.
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("io.lettuce.core.RedisCommandTimeoutException")
                .pattern("io.lettuce.core.api.sync.RedisStringCommands/(get|set)\\b");
        filibusterAnalysisConfigurationBuilderRedisExceptions.exception("io.lettuce.core.RedisCommandTimeoutException", createRedisErrorMap());
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        // CRDB.
        // TODO

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
