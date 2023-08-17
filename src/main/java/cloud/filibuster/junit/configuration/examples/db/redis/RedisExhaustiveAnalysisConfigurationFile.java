package cloud.filibuster.junit.configuration.examples.db.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class RedisExhaustiveAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createErrorMap(String cause) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("code", "");
        return myMap;
    }

    private static Map<String, String> createFutureErrorMap(String cause, String injectOn) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("injectOn", injectOn);
        myMap.put("code", "");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        String[][] exceptions = new String[][]{
                {"java.lettuce.core.sync.command_timeout_exception", "io.lettuce.core.RedisCommandTimeoutException",
                        "(io.lettuce.core.api.sync.RedisStringCommands/get|" +
                                "io.lettuce.core.api.sync.RedisStringCommands/set|" +
                                "io.lettuce.core.api.sync.RedisHashCommands/hget|" +
                                "io.lettuce.core.api.sync.RedisHashCommands/hset|" +
                                "io.lettuce.core.api.sync.RedisHashCommands/hgetall)\\b",
                        "Command timed out after 100 millisecond(s)"},
                {"java.lettuce.core.sync.busy_exception", "io.lettuce.core.RedisBusyException",
                        "(io.lettuce.core.api.sync.RedisServerCommands/flushall|" +
                                "io.lettuce.core.api.sync.RedisServerCommands/flushdb)\\b",
                        "BUSY Redis is busy running a script. You can only call SCRIPT KILL or SHUTDOWN NOSAVE"},
                {"java.lettuce.core.sync.command_execution_exception", "io.lettuce.core.RedisCommandExecutionException",
                        "(io.lettuce.core.api.sync.RedisHashCommands/hget|" +
                                "io.lettuce.core.api.sync.RedisHashCommands/hset|" +
                                "io.lettuce.core.api.sync.RedisHashCommands/hgetall)\\b",
                        "WRONGTYPE Operation against a key holding the wrong kind of value"},
                {"java.lettuce.core.sync.command_interrupted_exception", "io.lettuce.core.RedisCommandInterruptedException",
                        "(io.lettuce.core.RedisFuture/await)\\b",
                        "Command interrupted"},
                {"java.lettuce.core.sync.unknown_partition_exception", "io.lettuce.core.cluster.UnknownPartitionException",
                        "(io.lettuce.core.cluster.RedisClusterClient/getConnection|" +
                                "io.lettuce.core.cluster.PooledClusterConnectionProvider/getConnection)\\b",
                        "Connection not allowed. This partition is not known in the cluster view"},
                {"java.lettuce.core.sync.partition_selector_exception", "io.lettuce.core.cluster.PartitionSelectorException",
                        "(io.lettuce.core.cluster.RedisClusterClient/getConnection|" +
                                "io.lettuce.core.cluster.PooledClusterConnectionProvider/getConnection)\\b",
                        "Cannot determine a partition to read for slot"},
                {"java.lettuce.core.batch.exception", "io.lettuce.core.dynamic.batch.BatchException",
                        "(io.lettuce.core.dynamic.SimpleBatcher/flush)\\b",
                        "Error during batch command execution"},
                {"java.lettuce.core.connection.exception", "io.lettuce.core.RedisConnectionException",
                        "(io.lettuce.core.ConnectionFuture/get)\\b",
                        "Unable to connect"}
        };

        for (String[] exception : exceptions) {
            createException(filibusterCustomAnalysisConfigurationFileBuilder, exception[0], exception[1], exception[2], exception[3]);
        }


        // RedisFuture exceptions
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder().name("java.lettuce.core.async.command_timeout_exception")
                .pattern("(io.lettuce.core.api.async.RedisStringAsyncCommands/get|" +
                        "io.lettuce.core.api.async.RedisStringAsyncCommands/set)\\b");
        filibusterAnalysisConfigurationBuilderRedisExceptions.exception("io.lettuce.core.RedisCommandTimeoutException", createFutureErrorMap("Command timed out after 100 millisecond(s)",
                "java.util.concurrent.Future/get"));
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());


        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    private static void createException(FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder, String configName, String exceptionName, String pattern, String cause) {

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder().name(configName).pattern(pattern);

        filibusterAnalysisConfigurationBuilderRedisExceptions.exception(exceptionName, createErrorMap(cause));

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
