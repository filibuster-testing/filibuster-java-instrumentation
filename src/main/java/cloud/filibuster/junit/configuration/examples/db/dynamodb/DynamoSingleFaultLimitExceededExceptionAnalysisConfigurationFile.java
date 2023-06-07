package cloud.filibuster.junit.configuration.examples.db.dynamodb;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class DynamoSingleFaultLimitExceededExceptionAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createErrorMap() {
        Map<String, String> myMap = new HashMap<>();
        myMap.put("cause", "Throughput exceeds the current throughput limit for your account. Please contact AWS Support at " +
                "https://aws.amazon.com/support request a limit increase");
        myMap.put("code", "400");
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.dynamo.exceptions.RequestLimitExceededException")
                .pattern("software.amazon.awssdk.services.dynamodb.DynamoDbClient/*\\b");

        filibusterAnalysisConfigurationBuilderRedisExceptions.exception("software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException", createErrorMap());

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
