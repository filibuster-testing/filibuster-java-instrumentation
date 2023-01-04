package cloud.filibuster.junit.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilibusterGrpcExhaustiveAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createGrpcErrorMap(String code) {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", "");
        myMap.put("code", code);
        return myMap;
    }

    static {
        exhaustiveGrpcErrorCodeList.add("CANCELLED");
        exhaustiveGrpcErrorCodeList.add("UNKNOWN");
        exhaustiveGrpcErrorCodeList.add("INVALID_ARGUMENT");
        exhaustiveGrpcErrorCodeList.add("DEADLINE_EXCEEDED");
        exhaustiveGrpcErrorCodeList.add("NOT_FOUND");
        exhaustiveGrpcErrorCodeList.add("ALREADY_EXISTS");
        exhaustiveGrpcErrorCodeList.add("PERMISSION_DENIED");
        exhaustiveGrpcErrorCodeList.add("RESOURCE_EXHAUSTED");
        exhaustiveGrpcErrorCodeList.add("FAILED_PRECONDITION");
        exhaustiveGrpcErrorCodeList.add("ABORTED");
        exhaustiveGrpcErrorCodeList.add("OUT_OF_RANGE");
        exhaustiveGrpcErrorCodeList.add("UNIMPLEMENTED");
        exhaustiveGrpcErrorCodeList.add("INTERNAL");
        exhaustiveGrpcErrorCodeList.add("UNAVAILABLE");
        exhaustiveGrpcErrorCodeList.add("DATA_LOSS");
        exhaustiveGrpcErrorCodeList.add("UNAUTHENTICATED");

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)");

        for (String code : exhaustiveGrpcErrorCodeList) {
            filibusterAnalysisConfigurationBuilder.exception("io.grpc.StatusRuntimeException", createGrpcErrorMap(code));
        }

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
