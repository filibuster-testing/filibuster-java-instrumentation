package cloud.filibuster.junit.configuration;

import cloud.filibuster.instrumentation.datatypes.Pair;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilibusterAnalysisConfiguration {
    public enum MatcherType { SERVICE, METHOD }

    private final JSONObject analysisConfiguration = new JSONObject();
    private final JSONObject configurationObject = new JSONObject();
    private final List<JSONObject> exceptionFaultObjects = new ArrayList<>();
    private final List<JSONObject> errorFaultObjects = new ArrayList<>();
    private final List<JSONObject> latencyFaultObjects = new ArrayList<>();
    private final String name;
    private final String pattern;

    @SuppressWarnings("Varifier")
    public FilibusterAnalysisConfiguration(Builder builder) {
        this.name = builder.name;
        this.pattern = builder.pattern;

        configurationObject.put("pattern", builder.pattern);

        if (builder.exceptions.size() > 0) {
            configurationObject.put("exceptions", builder.exceptions);

            for (JSONObject exceptionObject : builder.exceptions) {
                JSONObject exception = new JSONObject();
                exception.put("forced_exception", exceptionObject);
                exceptionFaultObjects.add(exception);
            }
        }

        if (builder.errors.size() > 0) {
            configurationObject.put("errors", builder.errors);

            for (JSONObject errorObject : builder.errors) {
                JSONObject error = new JSONObject();
                error.put("failure_metadata", errorObject);
                errorFaultObjects.add(error);
            }
        }

        if (builder.latencies.size() > 0) {
            configurationObject.put("latencies", builder.latencies);

            for (JSONObject latencyObject : builder.latencies) {
                JSONObject latency = new JSONObject();
                latency.put("latency", latencyObject);
                latencyFaultObjects.add(latency);
            }
        }

        analysisConfiguration.put(builder.name, configurationObject);
    }

    public List<JSONObject> getExceptionFaultObjects() {
        return this.exceptionFaultObjects;
    }

    public List<JSONObject> getErrorFaultObjects() {
        return this.errorFaultObjects;
    }

    public List<JSONObject> getLatencyFaultObjects() {
        return this.latencyFaultObjects;
    }

    public boolean isPatternMatch(String matchString) {
        Pattern pattern = Pattern.compile(this.pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(matchString);
        return matcher.find();
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
        private final List<JSONObject> errors = new ArrayList<>();
        private final List<JSONObject> latencies = new ArrayList<>();

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
        public Builder error(String serviceName, List<JSONObject> types) {
            JSONObject error = new JSONObject();
            error.put("service_name", serviceName);
            error.put("types", types);
            errors.add(error);
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

        @CanIgnoreReturnValue
        public Builder latency(MatcherType matcherType, String matcher, int milliseconds) {
            JSONObject latency = new JSONObject();
            latency.put("type", matcherType.toString());
            latency.put("matcher", matcher);
            latency.put("milliseconds", milliseconds);
            latencies.add(latency);
            return this;
        }

        public FilibusterAnalysisConfiguration build() {
            return new FilibusterAnalysisConfiguration(this);
        }
    }
}