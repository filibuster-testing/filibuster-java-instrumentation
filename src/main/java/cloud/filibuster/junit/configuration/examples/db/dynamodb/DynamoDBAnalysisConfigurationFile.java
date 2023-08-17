package cloud.filibuster.junit.configuration.examples.db.dynamodb;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class DynamoDBAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createErrorMap(String cause) {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("code", "400");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        String[][] exceptions = new String[][]{
                {"java.dynamodb.exceptions.RequestLimitExceededException", "software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException",
                        "(software.amazon.awssdk.services.dynamodb.DynamoDbClient/*)\\b", "Throughput exceeds the current throughput limit for your account. Please contact AWS Support at " +
                        "https://aws.amazon.com/support request a limit increase"},
                {"java.dynamodb.exceptions.SdkClientException", "software.amazon.awssdk.services.dynamodb.model.SdkClientException",
                        "(software.amazon.awssdk.services.dynamodb.DynamoDbClient/*)\\b",
                        "Unable to load region information from any provider in the chain"},
                {"java.dynamodb.exceptions.DynamoDbException", "software.amazon.awssdk.services.dynamodb.model.DynamoDbException",
                        "(software.amazon.awssdk.services.dynamodb.DynamoDbClient/*)\\b",
                        "Validation errors detected"},
                {"java.dynamodb.exceptions.AwsServiceException", "software.amazon.awssdk.services.dynamodb.model.AwsServiceException",
                        "(software.amazon.awssdk.services.dynamodb.DynamoDbClient/*)\\b",
                        ""},
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
