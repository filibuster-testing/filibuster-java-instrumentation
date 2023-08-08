package cloud.filibuster.junit.configuration.examples.db.redis;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class RedisTrackedFunctionAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createFutureErrorMap(String cause, String injectOn, String trackInvocationOf) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("trackInvocationOf", trackInvocationOf);
        myMap.put("injectOn", injectOn);
        myMap.put("code", "");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // RedisFuture exceptions
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder().name("java.lettuce.core.async.command_timeout_exception")
                .pattern("(io.lettuce.core.api.async.RedisStringAsyncCommands/get|" +
                        "io.lettuce.core.api.async.RedisStringAsyncCommands/set)\\b");

        filibusterAnalysisConfigurationBuilderRedisExceptions.exception("io.lettuce.core.RedisCommandTimeoutException", createFutureErrorMap("Command timed out after 100 millisecond(s)",
                "java.util.concurrent.Future/get",
                "(java.util.concurrent.CompletionStage/thenAccept|java.util.concurrent.CompletionStage/thenApply)"));

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
