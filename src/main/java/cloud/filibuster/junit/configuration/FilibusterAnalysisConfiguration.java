package cloud.filibuster.junit.configuration;

import cloud.filibuster.instrumentation.datatypes.Pair;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilibusterAnalysisConfiguration {
    private final JSONObject analysisConfiguration = new JSONObject();
    private final JSONObject configurationObject = new JSONObject();
    private final String name;

    public FilibusterAnalysisConfiguration(Builder builder) {
        this.name = builder.name;
        configurationObject.put("pattern", builder.pattern);
        configurationObject.put("exceptions", builder.exceptions);
        analysisConfiguration.put(builder.name, configurationObject);
    }

    public Map.Entry<String, JSONObject> toJSONPair() {
        return Pair.of(name, configurationObject);
    }

    @Override
    public String toString() {
        return analysisConfiguration.toString();
    }

    public static class Builder {
        private String name;
        private String pattern;
        private final List<JSONObject> exceptions = new ArrayList<>();

        @CanIgnoreReturnValue
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder exception(String name, Map<String, String> metadata) {
            JSONObject exception = new JSONObject();
            exception.put("name", name);
            exception.put("metadata", metadata);
            exceptions.add(exception);
            return this;
        }

        public FilibusterAnalysisConfiguration build() {
            return new FilibusterAnalysisConfiguration(this);
        }
    }
}