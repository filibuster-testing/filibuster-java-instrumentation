package cloud.filibuster.junit.configuration.examples.db.cassandra;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class CassandraSingleFaultOverloadedExceptionAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
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
                {"java.cassandra.exceptions.OverloadedException", "com.datastax.oss.driver.api.core.servererrors.OverloadedException",
                        "(com.datastax.oss.driver.api.core.CqlSession/execute)\\b", "Queried host was overloaded: I'm busy"},
                {"java.cassandra.exceptions.InvalidQueryException", "com.datastax.oss.driver.api.core.servererrors.InvalidQueryException",
                        "(com.datastax.oss.driver.api.core.CqlSession/execute|" +
                                "com.datastax.oss.driver.api.core.CqlSession/executeAsync|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepare|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepareAsync/)\\b", ""},
                {"java.cassandra.exceptions.ReadFailureException", "com.datastax.oss.driver.api.core.servererrors.ReadFailureException",
                        "(com.datastax.oss.driver.api.core.CqlSession/execute|" +
                                "com.datastax.oss.driver.api.core.CqlSession/executeAsync|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepare|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepareAsync/)\\b", ""},
                {"java.cassandra.exceptions.ReadTimeoutException", "com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException",
                        "(com.datastax.oss.driver.api.core.CqlSession/execute|" +
                                "com.datastax.oss.driver.api.core.CqlSession/executeAsync|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepare|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepareAsync/)\\b", ""},
                {"java.cassandra.exceptions.WriteTimeoutException", "com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException",
                        "(com.datastax.oss.driver.api.core.CqlSession/execute|" +
                                "com.datastax.oss.driver.api.core.CqlSession/executeAsync|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepare|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepareAsync/)\\b", ""},
                {"java.cassandra.exceptions.WriteFailureException", "com.datastax.oss.driver.api.core.servererrors.WriteFailureException",
                        "(com.datastax.oss.driver.api.core.CqlSession/execute|" +
                                "com.datastax.oss.driver.api.core.CqlSession/executeAsync|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepare|" +
                                "com.datastax.oss.driver.api.core.CqlSession/prepareAsync/)\\b", ""}
        };

        for (String[] exception : exceptions) {
            createException(filibusterCustomAnalysisConfigurationFileBuilder, exception[0], exception[1], exception[2], exception[3]);
        }

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    private static void createException(FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder, String configName, String exceptionName, String pattern, String cause) {

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderExceptions = new FilibusterAnalysisConfiguration.Builder().name(configName).pattern(pattern);

        filibusterAnalysisConfigurationBuilderExceptions.exception(exceptionName, createErrorMap(cause));

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderExceptions.build());
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
