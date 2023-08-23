package cloud.filibuster.junit.configuration.examples.db.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.transformers.DBException;
import cloud.filibuster.junit.server.core.transformers.DBExceptionTransformer;

import java.util.HashMap;
import java.util.Map;

public class RedisTransformerExceptionsAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createErrorMap() {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", "Command timed out after 100 millisecond(s)");
        myMap.put("code", "");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.lettuce.core.RedisCommandTimeoutException")
                .pattern("io.lettuce.core.api.sync.RedisStringCommands/(get|set)\\b");

        DBException.Builder commandTimeoutBuilder = new DBException.Builder();
        commandTimeoutBuilder.name("io.lettuce.core.RedisCommandTimeoutException");
        commandTimeoutBuilder.metadata(createErrorMap());
        DBException commandTimeoutException = commandTimeoutBuilder.build();

        DBExceptionTransformer.addDbException(commandTimeoutException);

        filibusterAnalysisConfigurationBuilderRedisExceptions.transformer(DBExceptionTransformer.class);

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
