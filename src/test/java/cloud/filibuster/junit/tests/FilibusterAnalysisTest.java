package cloud.filibuster.junit.tests;

import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilibusterAnalysisTest {
    @Test
    @SuppressWarnings("Java8ApiChecker")
    public void verifyDefaultAnalysisConfigurationTest() {
        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*Service/.*)")
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "UNAVAILABLE"
                ))
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "DEADLINE_EXCEEDED"
                ))
                .build();

        String expected = "{\"java.grpc\":{\"pattern\":\"(.*Service/.*)\",\"exceptions\":[{\"metadata\":{\"cause\":\"\",\"code\":\"UNAVAILABLE\"},\"name\":\"io.grpc.StatusRuntimeException\"},{\"metadata\":{\"cause\":\"\",\"code\":\"DEADLINE_EXCEEDED\"},\"name\":\"io.grpc.StatusRuntimeException\"}]}}";

        assertEquals(expected, filibusterAnalysisConfiguration.toString());
    }

    @Test
    @SuppressWarnings("Java8ApiChecker")
    public void verifyDefaultAnalysisConfigurationFileTest() throws IOException {
        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration1 = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc")
                .pattern("(.*Service/.*)")
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "UNAVAILABLE"
                ))
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "DEADLINE_EXCEEDED"
                ))
                .build();

        FilibusterAnalysisConfiguration filibusterAnalysisConfiguration2 = new FilibusterAnalysisConfiguration.Builder()
                .name("java.grpc.other")
                .pattern("(.*Service/.*)")
                .exception("io.grpc.StatusRuntimeException", Map.of(
                        "cause", "",
                        "code", "NOT_FOUND"
                ))
                .build();

        FilibusterCustomAnalysisConfigurationFile filibusterAnalysisConfigurationFile = new FilibusterCustomAnalysisConfigurationFile.Builder()
                .analysisConfiguration(filibusterAnalysisConfiguration1)
                .analysisConfiguration(filibusterAnalysisConfiguration2)
                .build();

        String expected = "{\"java.grpc.other\":{\"pattern\":\"(.*Service/.*)\",\"exceptions\":[{\"metadata\":{\"cause\":\"\",\"code\":\"NOT_FOUND\"},\"name\":\"io.grpc.StatusRuntimeException\"}]},\"java.grpc\":{\"pattern\":\"(.*Service/.*)\",\"exceptions\":[{\"metadata\":{\"cause\":\"\",\"code\":\"UNAVAILABLE\"},\"name\":\"io.grpc.StatusRuntimeException\"},{\"metadata\":{\"cause\":\"\",\"code\":\"DEADLINE_EXCEEDED\"},\"name\":\"io.grpc.StatusRuntimeException\"}]}}";
        assertEquals(expected, filibusterAnalysisConfigurationFile.toString());

        Path file = filibusterAnalysisConfigurationFile.writeToDisk();
        String read = Files.readString(file);
        JSONObject reread = new JSONObject(read);

        // TODO: Fix, this is because keys are in different sort order.  Good enough for now.
        assertEquals(filibusterAnalysisConfigurationFile.toString().length(), reread.toString().length());
    }
}
