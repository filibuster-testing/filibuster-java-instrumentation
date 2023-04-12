package cloud.filibuster.junit.configuration;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public interface FilibusterAnalysisConfigurationFile {
    FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile();

    static Map<String, String> createGrpcErrorMap(String code, String cause, String description) {
        return createGrpcErrorMap(code, cause, description, /* causeMessage= */ null);
    }

    static Map<String, String> createGrpcErrorMap(String code, String cause, String description, @Nullable String causeMessage) {
        Map<String,String> myMap = new HashMap<>();

        if (cause != null) {
            myMap.put("cause", cause);
        } else {
            myMap.put("cause", "");
        }

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

        if (causeMessage != null) {
            myMap.put("cause_message", causeMessage);
        } else {
            myMap.put("cause_message", "");
        }

        return myMap;
    }
}
