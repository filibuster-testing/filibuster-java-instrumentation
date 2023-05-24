package cloud.filibuster.junit.configuration.examples.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;

public class RedisExhaustiveAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createErrorMap(String cause) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("code", "");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        String[][] exceptions = new String[][]{
                {"io.lettuce.core.RedisCommandTimeoutException",
                        "/(io.lettuce.core.api.async.RedisStringAsyncCommands.get|" +
                                "io.lettuce.core.api.sync.RedisStringCommands.get|" +
                                "io.lettuce.core.api.sync.RedisStringCommands.set|" +
                                "io.lettuce.core.api.sync.RedisHashCommands.hget|" +
                                "io.lettuce.core.api.sync.RedisHashCommands.hset|" +
                                "io.lettuce.core.api.sync.RedisHashCommands.hgetall)\\b",
                        "Command timed out after 100 millisecond(s)"},
                {"io.lettuce.core.RedisConnectionException",
                        "/(io.lettuce.core.api.StatefulRedisConnection.sync|" +
                                "io.lettuce.core.api.StatefulRedisConnection.async)\\b",
                        "Connection closed prematurely"},
                {"io.lettuce.core.RedisBusyException",
                        "/(io.lettuce.core.api.sync.RedisServerCommands.flushall|" +
                                "io.lettuce.core.api.sync.RedisServerCommands.flushdb)\\b",
                        "BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE"},
                {"io.lettuce.core.RedisCommandExecutionException",
                        "/(io.lettuce.core.api.sync.RedisHashCommands.hget|" +
                                "io.lettuce.core.api.sync.RedisHashCommands.hset|" +
                                "io.lettuce.core.api.sync.RedisHashCommands.hgetall)\\b",
                        "WRONGTYPE Operation against a key holding the wrong kind of value"},
                {"io.lettuce.core.RedisCommandInterruptedException",
                        "/(io.lettuce.core.RedisFuture.await)\\b",
                        "Command interrupted"},
                {"io.lettuce.core.cluster.UnknownPartitionException",
                        "/(io.lettuce.core.cluster.RedisClusterClient.getConnection|" +
                                "io.lettuce.core.cluster.PooledClusterConnectionProvider.getConnection)\\b",
                        "Connection not allowed. This partition is not known in the cluster view"},
                {"io.lettuce.core.cluster.PartitionSelectorException",
                        "/(io.lettuce.core.cluster.RedisClusterClient.getConnection|" +
                                "io.lettuce.core.cluster.PooledClusterConnectionProvider.getConnection)\\b",
                        "Cannot determine a partition to read for slot"},
                {"io.lettuce.core.dynamic.batch.BatchException",
                        "/(io.lettuce.core.dynamic.SimpleBatcher.flush)\\b",
                        "Error during batch command execution"},
        };

        for (String[] exception : exceptions) {
            createException(filibusterCustomAnalysisConfigurationFileBuilder, exception[0], exception[1], exception[2]);
        }

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    private static void createException(FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder, String name, String pattern, String cause) {

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder().name(name).pattern(REDIS_MODULE_NAME + pattern);

        filibusterAnalysisConfigurationBuilderRedisExceptions.exception(name, createErrorMap(cause));

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
