package cloud.filibuster.junit.configuration;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilibusterCustomAnalysisConfigurationFile {
    private static final Logger logger = Logger.getLogger(FilibusterCustomAnalysisConfigurationFile.class.getName());

    private final JSONObject analysisConfigurationFile = new JSONObject();

    public FilibusterCustomAnalysisConfigurationFile(Builder builder) {
        for (FilibusterAnalysisConfiguration analysisConfiguration : builder.analysisConfigurations) {
            Map.Entry<String, JSONObject> entry = analysisConfiguration.toJSONPair();
            analysisConfigurationFile.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String toString() {
        return analysisConfigurationFile.toString();
    }

    @SuppressWarnings("DefaultCharset")
    public boolean writeToDisk(String filePath) {
        try {
            FileWriter fw = new FileWriter(filePath);
            fw.write(analysisConfigurationFile.toString());
            fw.close();
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "couldn't write analysis file!");
            return false;
        }
    }

    @SuppressWarnings("DefaultCharset")
    public Path writeToDisk() throws IOException {
        File f = File.createTempFile("filibuster-analysis", null);
        FileWriter fw = new FileWriter(f);
        fw.write(analysisConfigurationFile.toString());
        fw.close();
        return f.toPath();
    }

    public static class Builder {
        private final List<FilibusterAnalysisConfiguration> analysisConfigurations = new ArrayList<>();

        @CanIgnoreReturnValue
        public Builder analysisConfiguration(FilibusterAnalysisConfiguration analysisConfiguration) {
            analysisConfigurations.add(analysisConfiguration);
            return this;
        }

        public FilibusterCustomAnalysisConfigurationFile build() {
            return new FilibusterCustomAnalysisConfigurationFile(this);
        }
    }
}
