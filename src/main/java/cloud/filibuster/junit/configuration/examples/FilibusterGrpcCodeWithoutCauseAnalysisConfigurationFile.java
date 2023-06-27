package cloud.filibuster.junit.configuration.examples;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class FilibusterGrpcCodeWithoutCauseAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    static Map<String, String> createGrpcErrorMap(String code, String description) {
        Map<String,String> myMap = new HashMap<>();

        if (code != null) {
            myMap.put("code", code);
        } else {
            myMap.put("code", "");
        }

        if (description != null) {
            myMap.put("description", description);
        } else {
            myMap.put("description", "");
        }

        return myMap;
    }

    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        String code = "DATA_LOSS";
        String description = "This is a fake error generated by Filibuster.";

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)")
                .type("grpc")
                .exception("io.grpc.StatusRuntimeException", createGrpcErrorMap(code, description));
        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = filibusterAnalysisConfigurationBuilder.build();
        filibusterCustomAnalysisConfigurationFile = new FilibusterCustomAnalysisConfigurationFile.Builder()
                .analysisConfiguration(filibusterAnalysisConfiguration)
                .build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
