package cloud.filibuster.junit.configuration.examples.db.postgresql;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfigurationFile;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.transformers.BitInByteArrTransformer;
import cloud.filibuster.junit.server.core.transformers.BooleanAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.IntegerAsStringTransformer;
import cloud.filibuster.junit.server.core.transformers.StringTransformer;
import cloud.filibuster.junit.server.core.transformers.Transformer;

public class PostgresTransformStringAnalysisConfigurationFile implements FilibusterAnalysisConfigurationFile {
    private static final FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    static {
        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        Object[][] faults = new Object[][]{
                {"java.transformers.transformer.postgres_execute",
                        BooleanAsStringTransformer.class,
                        "java.sql.PreparedStatement/execute"},
                {"java.transformers.transformer.postgres_executeUpdate",
                        IntegerAsStringTransformer.class,
                        "java.sql.PreparedStatement/executeUpdate"},
                {"java.transformers.transformer.postgres_getInt",
                        IntegerAsStringTransformer.class,
                        "java.sql.ResultSet/getInt"},
                {"java.transformers.transformer.postgres_getBoolean",
                        BooleanAsStringTransformer.class,
                        "java.sql.ResultSet/getBoolean"},
                {"java.transformers.transformer.postgres_getString",
                        StringTransformer.class,
                        "java.sql.ResultSet/getString"},
                {"java.transformers.transformer.postgres_getBytes",
                        BitInByteArrTransformer.class,
                        "java.sql.ResultSet/getBytes"},
        };

        for (Object[] fault : faults) {
            @SuppressWarnings("unchecked")
            Class<? extends Transformer<?, ?>> transformerClass = (Class<? extends Transformer<?, ?>>) fault[1];
            createTransformerFault(filibusterCustomAnalysisConfigurationFileBuilder, (String) fault[0], transformerClass, (String) fault[2]);
        }

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }


    private static void createTransformerFault(FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder, String configName, Class<? extends Transformer<?, ?>> transformerClass, String pattern) {

        FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilderRedisExceptions = new FilibusterAnalysisConfiguration.Builder().name(configName).pattern(pattern);

        filibusterAnalysisConfigurationBuilderRedisExceptions.transformer(transformerClass);

        filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfigurationBuilderRedisExceptions.build());
    }

    @Override
    public FilibusterCustomAnalysisConfigurationFile toFilibusterCustomAnalysisConfigurationFile() {
        return filibusterCustomAnalysisConfigurationFile;
    }
}
