package cloud.filibuster.junit.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilibusterSingleFaultUnimplementedAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createGrpcErrorMap(String code) {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", "");
        myMap.put("code", code);
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // Google's gRPC exception types.
        // Taken from: https://grpc.github.io/grpc/core/md_doc_statuscodes.html
        exhaustiveGrpcErrorCodeList.add("UNIMPLEMENTED");

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderGrpcExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.exceptions")
                .pattern("(.*/.*)");
        for (String code : exhaustiveGrpcErrorCodeList) {
            filibusterAnalysisConfigurationBuilderGrpcExceptions.exception("io.grpc.StatusRuntimeException", createGrpcErrorMap(code));
        }
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderGrpcExceptions.build());

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
