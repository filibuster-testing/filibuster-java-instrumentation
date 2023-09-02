package cloud.filibuster.junit.configuration.examples;

import cloud.filibuster.RpcType;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilibusterDefaultAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final List<String> exhaustiveGrpcErrorCodeList = new ArrayList<>();
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createGrpcErrorMap() {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", "");
        myMap.put("code", "");
        return myMap;
    }

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
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // Google's gRPC exception types.
        // Taken from: https://grpc.github.io/grpc/core/md_doc_statuscodes.html
        exhaustiveGrpcErrorCodeList.add("UNIMPLEMENTED");
        exhaustiveGrpcErrorCodeList.add("INTERNAL");
        exhaustiveGrpcErrorCodeList.add("UNAVAILABLE");
        exhaustiveGrpcErrorCodeList.add("DEADLINE_EXCEEDED");
        exhaustiveGrpcErrorCodeList.add("UNKNOWN");

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderGrpcExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.exceptions")
                .pattern("(.*/.*)")
                .rpcType(RpcType.GRPC);
        for (String code : exhaustiveGrpcErrorCodeList) {
            filibusterAnalysisConfigurationBuilderGrpcExceptions.exception("io.grpc.StatusRuntimeException", createGrpcErrorMap(code));
        }
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderGrpcExceptions.build());

        // Google's gRPC error types.
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderGrpcErrors = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.errors")
                .pattern("(.*/.*)")
                .rpcType(RpcType.GRPC);

        // Specification of error code without specification of the exception types
        // that will encapsulate the error codes.
        //
        // This should only be used when testing other languages where we need to raise
        // but don't know the target implementation.
        //
        List<String> grpcErrorCodes = new ArrayList<>();
//        grpcErrorCodes.add("NOT_FOUND");

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
        filibusterAnalysisConfigurationBuilderHttpExceptions.exception("com.linecorp.armeria.client.ResponseTimeoutException", createGrpcErrorMap());

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

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
