package cloud.filibuster.junit.configuration.examples.db.postgresql;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class PostgresAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
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
                {"java.postgresql.connection.exceptions.getSchema", "org.postgresql.util.PSQLException",
                        "(java.sql.Connection/getSchema)\\b", ""},
                {"java.postgresql.driver.exceptions.connect.loading_error", "org.postgresql.util.PSQLException",
                        "(java.sql.Driver/connect)\\b", "Error loading default settings from driverconfig.properties"},
                {"java.postgresql.driver.exceptions.connect.security_error", "org.postgresql.util.PSQLException",
                        "(java.sql.Driver/connect)\\b", "Your security policy has prevented the connection from being attempted.  You probably need to grant the connect java.net.SocketPermission to the database server host and port that you wish to connect to."},
                {"java.postgresql.driver.exceptions.connect.connection_error", "org.postgresql.util.PSQLException",
                        "(java.sql.Driver/connect)\\b", "Something unusual has occurred to cause the driver to fail. Please report this exception."},
                {"java.postgresql.Connection.exceptions.sql_exception", "java.sql.SQLException",
                        "(java.sql.Connection/prepareStatement|" +
                                "java.sql.Connection/createStatement)\\b", ""},
                {"java.postgresql.preparedStatement.exceptions.sql_exception", "java.sql.SQLException",
                        "(java.sql.PreparedStatement/executeQuery|" +
                                "java.sql.PreparedStatement/execute|" +
                                "java.sql.PreparedStatement/executeUpdate)\\b", ""}
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
