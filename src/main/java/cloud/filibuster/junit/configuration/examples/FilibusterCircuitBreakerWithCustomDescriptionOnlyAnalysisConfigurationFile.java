package cloud.filibuster.junit.configuration.examples;

import cloud.filibuster.RpcType;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;

public class FilibusterCircuitBreakerWithCustomDescriptionOnlyAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        String code = "UNKNOWN";
        String cause = "cloud.filibuster.exceptions.CircuitBreakerException";
        String causeMessage = "Circuit breaker prevented this request.";
        String description = "";

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)")
                .rpcType(RpcType.GRPC)
                .exception("io.grpc.StatusRuntimeException", FilibusterAnalysisConfigurationFile.createGrpcErrorMap(code, cause, description, causeMessage));
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
