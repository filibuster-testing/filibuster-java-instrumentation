package cloud.filibuster.junit.configuration.examples;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilibusterWorldExtendedDefaultAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createGrpcErrorMap(String code) {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", "");
        myMap.put("code", code);
        return myMap;
    }

    private static Map<String, String> createHttpErrorMap(String cause) {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", cause);
        myMap.put("code", "");
        return myMap;
    }

    static {
        // Configuration file.
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // Google's gRPC exception types.
        exhaustiveGrpcErrorCodeList.add("DEADLINE_EXCEEDED");
        exhaustiveGrpcErrorCodeList.add("UNAVAILABLE");

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderGrpcExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.exceptions")
                .pattern("(.*/.*)");
        for (String code : exhaustiveGrpcErrorCodeList) {
            filibusterAnalysisConfigurationBuilderGrpcExceptions.exception("io.grpc.StatusRuntimeException", createGrpcErrorMap(code));
        }
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderGrpcExceptions.build());

        // Google's gRPC error types.
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderGrpcErrors = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.errors")
                .pattern("(.*/.*)");

        List<String> grpcErrorCodes = new ArrayList<>();
        grpcErrorCodes.add("UNIMPLEMENTED");
        grpcErrorCodes.add("INTERNAL");

        List<JSONObject> grpcErrorTypes = new ArrayList<>();
        for (String errorCode : grpcErrorCodes) {
            JSONObject codeObject = new JSONObject();
            codeObject.put("code", errorCode);

            JSONObject metadataObject = new JSONObject();
            metadataObject.put("metadata", codeObject);

            JSONObject exceptionObject = new JSONObject();
            exceptionObject.put("exception", metadataObject);

            grpcErrorTypes.add(exceptionObject);
        }

        filibusterAnalysisConfigurationBuilderGrpcErrors.error(".*", grpcErrorTypes);
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderGrpcErrors.build());

        // Armeria's WebClient exception types.
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderHttpExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.WebClient.exceptions")
                .pattern("WebClient\\.(GET|PUT|POST|HEAD)");
        filibusterAnalysisConfigurationBuilderHttpExceptions.exception("com.linecorp.armeria.client.UnprocessedRequestException", createHttpErrorMap("io.netty.channel.ConnectTimeoutException"));
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderHttpExceptions.build());

        // Armeria's WebClient error types.
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderHttpErrors = new FilibusterAnalysisConfiguration.Builder()
                .name("java.WebClient.errors")
                .pattern("WebClient\\.(GET|PUT|POST|HEAD)");

        List<String> httpErrorCodes = new ArrayList<>();
        httpErrorCodes.add("500");
        httpErrorCodes.add("502");
        httpErrorCodes.add("503");

        List<JSONObject> webClientErrorTypes = new ArrayList<>();
        for (String errorCode : httpErrorCodes) {
            JSONObject statusCodeObject = new JSONObject();
            statusCodeObject.put("status_code", errorCode);

            JSONObject returnValueObject = new JSONObject();
            returnValueObject.put("return_value", statusCodeObject);

            webClientErrorTypes.add(returnValueObject);
        }
        filibusterAnalysisConfigurationBuilderHttpErrors.error(".*", webClientErrorTypes);
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderHttpErrors.build());

        // Armeria's WebClient error types (for world service.)
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderHttpErrorsForWorld = new FilibusterAnalysisConfiguration.Builder()
                .name("java.WebClient.world.errors")
                .pattern("WebClient\\.(GET|PUT|POST|HEAD)");

        List<String> worldHttpErrorCodes = new ArrayList<>();
        worldHttpErrorCodes.add("404");

        List<JSONObject> worldWebClientErrorTypes = new ArrayList<>();
        for (String errorCode : worldHttpErrorCodes) {
            JSONObject statusCodeObject = new JSONObject();
            statusCodeObject.put("status_code", errorCode);

            JSONObject returnValueObject = new JSONObject();
            returnValueObject.put("return_value", statusCodeObject);

            worldWebClientErrorTypes.add(returnValueObject);
        }
        filibusterAnalysisConfigurationBuilderHttpErrorsForWorld.error("world", worldWebClientErrorTypes);
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderHttpErrorsForWorld.build());

        // Generate configuration file.
        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
