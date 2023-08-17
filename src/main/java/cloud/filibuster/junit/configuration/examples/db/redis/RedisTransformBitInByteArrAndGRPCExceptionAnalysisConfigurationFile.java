package cloud.filibuster.junit.configuration.examples.db.redis;

import cloud.filibuster.RpcType;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.transformers.BitInByteArrTransformer;

import java.util.HashMap;
import java.util.Map;

public class RedisTransformBitInByteArrAndGRPCExceptionAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private static Map<String, String> createGrpcErrorMap(String code) {
        Map<String,String> myMap = new HashMap<>();
        myMap.put("cause", "");
        myMap.put("code", code);
        return myMap;
    }

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        // Build Redis bit transformer exceptions
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder()
                .name("java.transformers.transform_bit_in_byte_arr.redis")
                .pattern("io.lettuce.core.api.sync.RedisStringCommands/get\\b");
        filibusterAnalysisConfigurationBuilderRedisExceptions.transformer(BitInByteArrTransformer.class);
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());

        // Build gRPC exceptions
        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)")
                .rpcType(RpcType.GRPC);
        filibusterAnalysisConfigurationBuilder.exception("io.grpc.StatusRuntimeException", createGrpcErrorMap("UNAVAILABLE"));
        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilder.build());


        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
