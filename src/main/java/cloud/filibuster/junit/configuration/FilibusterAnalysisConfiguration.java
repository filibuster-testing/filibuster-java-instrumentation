package cloud.filibuster.junit.configuration;

import cloud.filibuster.RpcType;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.server.core.transformers.Transformer;
import cloud.filibuster.junit.configuration.examples.db.byzantine.types.ByzantineFaultType;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONObject;

import javax.annotation.Nullable;
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
    private final List<JSONObject> byzantineFaultObjects = new ArrayList<>();
    private final List<JSONObject> transformerFaultObjects = new ArrayList<>();
    private final String name;
    private final String pattern;
    private final RpcType rpcType;

    @SuppressWarnings("Varifier")
    public FilibusterAnalysisConfiguration(Builder builder) {
        this.name = builder.name;
        this.pattern = builder.pattern;
        this.rpcType = builder.rpcType;

        configurationObject.put("pattern", builder.pattern);
        configurationObject.put("rpc_type", builder.rpcType);

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

        if (builder.byzantines.size() > 0) {
            configurationObject.put("byzantines", builder.byzantines);

            for (JSONObject byzantineObject : builder.byzantines) {
                JSONObject byzantine = new JSONObject();
                byzantine.put("byzantine_fault", byzantineObject);
                byzantineFaultObjects.add(byzantine);
            }
        }

        if (builder.transformers.size() > 0) {
            configurationObject.put("transformers", builder.transformers);

            for (JSONObject byzantineObject : builder.transformers) {
                JSONObject transformerJson = new JSONObject();
                transformerJson.put("transformer_fault", byzantineObject);
                transformerFaultObjects.add(transformerJson);
            }
        }

        analysisConfiguration.put(builder.name, configurationObject);
    }

    public List<JSONObject> getExceptionFaultObjects() {
        return this.exceptionFaultObjects;
    }

    public List<JSONObject> getByzantineFaultObjects() {
        return this.byzantineFaultObjects;
    }

    public List<JSONObject> getTransformerFaultObjects() {
        return this.transformerFaultObjects;
    }

    public List<JSONObject> getErrorFaultObjects() {
        return this.errorFaultObjects;
    }

    public List<JSONObject> getLatencyFaultObjects() {
        return this.latencyFaultObjects;
    }

    public boolean hasRpcType() {
        return this.rpcType != null;
    }

    public boolean isPatternMatch(String matchString) {
        Pattern pattern = Pattern.compile(this.pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(matchString);
        return matcher.find();
    }

    public boolean isRpcTypeMatch(RpcType rpcType) {
        return rpcType.equals(this.rpcType);
    }

    public Map.Entry<String, JSONObject> toJsonPair() {
        return Pair.of(name, configurationObject);
    }

    @Override
    public String toString() {
        return analysisConfiguration.toString();
    }

    public static class Builder {
        private String name;
        private String pattern;
        private RpcType rpcType;
        private final List<JSONObject> exceptions = new ArrayList<>();
        private final List<JSONObject> errors = new ArrayList<>();
        private final List<JSONObject> latencies = new ArrayList<>();
        private final List<JSONObject> byzantines = new ArrayList<>();
        private final List<JSONObject> transformers = new ArrayList<>();

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
        public <T> Builder byzantine(ByzantineFaultType<?> faultType, @Nullable T value) {
            JSONObject byzantine = new JSONObject();
            byzantine.put("type", faultType);
            // JSONObject does not accept keys with null values
            // Instead, we use JSONObject.NULL (https://developer.android.com/reference/org/json/JSONObject.html#NULL)
            byzantine.put("value", value != null ? value : JSONObject.NULL);
            byzantines.add(byzantine);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder transformer(Class<? extends Transformer<?, ?>> transformer) {
            JSONObject transformerJson = new JSONObject();
            transformerJson.put("transformerClassName", transformer.getName());
            transformers.add(transformerJson);
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

        @CanIgnoreReturnValue
        public Builder rpcType(RpcType type) {
            this.rpcType = type;
            return this;
        }

        public FilibusterAnalysisConfiguration build() {
            return new FilibusterAnalysisConfiguration(this);
        }
    }
}