package cloud.filibuster.junit.configuration;

public class FilibusterCircuitBreakerOnlyAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        String code = "UNKNOWN";
        String cause = "cloud.filibuster.exceptions.CircuitBreakerException";
        String description = "";

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*/.*)")
                .exception("io.grpc.StatusRuntimeException", FilibusterAnalysisConfigurationFile.createGrpcErrorMap(code, cause, description));
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
