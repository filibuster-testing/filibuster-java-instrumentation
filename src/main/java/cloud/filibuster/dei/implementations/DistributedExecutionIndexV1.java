package cloud.filibuster.dei.implementations;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexBase;
import cloud.filibuster.dei.DistributedExecutionIndexKey;
import cloud.filibuster.dei.DistributedExecutionIndexType;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Key.Builder;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static cloud.filibuster.dei.DistributedExecutionIndexType.V1;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Components.generateRpcAsynchronousComponentFromCallsite;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Components.generateRpcSignatureFromCallsite;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Components.generateRpcSourceFromCallsite;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Components.generateRpcSynchronousComponentFromCallsite;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Asynchronous.getAsynchronousDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Asynchronous.getAsynchronousInclude;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Signature.getSignatureDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Signature.getSignatureInclude;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Source.getSourceDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Source.getSourceInclude;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Synchronous.getSynchronousDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Synchronous.getSynchronousInclude;

import static cloud.filibuster.instrumentation.helpers.Hashing.createDigest;
import static cloud.filibuster.instrumentation.helpers.Property.getDeiFaultScopeCounterProperty;

public class DistributedExecutionIndexV1 extends DistributedExecutionIndexBase implements DistributedExecutionIndex {
    public static final DistributedExecutionIndexType VERSION = V1;

    public static class Properties {
        public static class Source {
            /***********************************************************************************
             ** filibuster.dei.v1.source.include
             ***********************************************************************************/

            private final static String SOURCE_INCLUDE = "filibuster.dei.v1.source.include";

            public static void setSourceInclude(boolean value) {
                System.setProperty(SOURCE_INCLUDE, String.valueOf(value));
            }

            public static boolean getSourceInclude() {
                String propertyValue = System.getProperty(SOURCE_INCLUDE);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }

            /***********************************************************************************
             ** filibuster.dei.v1.source.digest
             ***********************************************************************************/

            private final static String SOURCE_DIGEST = "filibuster.dei.v1.source.digest";

            public static void setSourceDigest(boolean value) {
                System.setProperty(SOURCE_DIGEST, String.valueOf(value));
            }

            public static boolean getSourceDigest() {
                String propertyValue = System.getProperty(SOURCE_DIGEST);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }
        }

        public static class Signature {
            /***********************************************************************************
             ** filibuster.dei.v1.signature.include
             ***********************************************************************************/

            private final static String SIGNATURE_INCLUDE = "filibuster.dei.v1.signature.include";

            public static void setSignatureInclude(boolean value) {
                System.setProperty(SIGNATURE_INCLUDE, String.valueOf(value));
            }

            public static boolean getSignatureInclude() {
                String propertyValue = System.getProperty(SIGNATURE_INCLUDE);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }

            /***********************************************************************************
             ** filibuster.dei.v1.signature.digest
             ***********************************************************************************/

            private final static String SIGNATURE_DIGEST = "filibuster.dei.v1.signature.digest";

            public static void setSignatureDigest(boolean value) {
                System.setProperty(SIGNATURE_DIGEST, String.valueOf(value));
            }

            public static boolean getSignatureDigest() {
                String propertyValue = System.getProperty(SIGNATURE_DIGEST);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }
        }

        public static class Synchronous {
            /***********************************************************************************
             ** filibuster.dei.v1.synchronous.include
             ***********************************************************************************/

            private final static String SYNCHRONOUS_INCLUDE = "filibuster.dei.v1.synchronous.include";

            public static void setSynchronousInclude(boolean value) {
                System.setProperty(SYNCHRONOUS_INCLUDE, String.valueOf(value));
            }

            public static boolean getSynchronousInclude() {
                String propertyValue = System.getProperty(SYNCHRONOUS_INCLUDE);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }

            /***********************************************************************************
             ** filibuster.dei.v1.synchronous.digest
             ***********************************************************************************/

            private final static String SYNCHRONOUS_DIGEST = "filibuster.dei.v1.synchronous.digest";

            public static void setSynchronousDigest(boolean value) {
                System.setProperty(SYNCHRONOUS_DIGEST, String.valueOf(value));
            }

            public static boolean getSynchronousDigest() {
                String propertyValue = System.getProperty(SYNCHRONOUS_DIGEST);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }
        }

        public static class Asynchronous {
            /***********************************************************************************
             ** filibuster.dei.v1.asynchronous.include
             ***********************************************************************************/

            private final static String ASYNCHRONOUS_INCLUDE = "filibuster.dei.v1.asynchronous.include";

            public static void setAsynchronousInclude(boolean value) {
                System.setProperty(ASYNCHRONOUS_INCLUDE, String.valueOf(value));
            }

            public static boolean getAsynchronousInclude() {
                String propertyValue = System.getProperty(ASYNCHRONOUS_INCLUDE);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }

            /***********************************************************************************
             ** filibuster.dei.v1.asynchronous.digest
             ***********************************************************************************/

            private final static String ASYNCHRONOUS_DIGEST = "filibuster.dei.v1.asynchronous.digest";

            public static void setAsynchronousDigest(boolean value) {
                System.setProperty(ASYNCHRONOUS_DIGEST, String.valueOf(value));
            }

            public static boolean getAsynchronousDigest() {
                String propertyValue = System.getProperty(ASYNCHRONOUS_DIGEST);

                if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                    return true;
                } else {
                    return Boolean.parseBoolean(propertyValue);
                }
            }
        }
    }

    public static class Key implements DistributedExecutionIndexKey {
        private final String source;
        private final String destination;
        private final String signature;
        private final String synchronous;
        private final String asynchronous;

        public Key(Builder builder) {
            this.source = builder.source;
            this.destination = builder.destination;
            this.signature = builder.signature;
            this.synchronous = builder.synchronous;
            this.asynchronous = builder.asynchronous;
        }

        @Override
        public String toString() {
            return serialize();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof Key)) {
                return false; }

            Key k = (Key) o;
            return Objects.equals(this.source, k.source) && Objects.equals(this.signature, k.signature) && Objects.equals(this.synchronous, k.synchronous) && Objects.equals(this.asynchronous, k.asynchronous);
        }

        @Override
        public String onlySignature() {
            return this.signature;
        }

        @Override
        public String onlyDestination() {
            return this.destination;
        }

        @Override
        public int hashCode() {
            return Objects.hash(VERSION, source, signature, synchronous, asynchronous);
        }

        @Override
        public String serialize() {
            ArrayList<String> deiElements = new ArrayList<>();
            deiElements.add(VERSION.name());
            deiElements.add(source);
            deiElements.add(signature);
            deiElements.add(synchronous);
            deiElements.add(asynchronous);
            return String.join("-", deiElements);
        }

        public static class Builder {
            private static final DistributedExecutionIndexType version = V1;
            private String source;
            private String destination;
            private String signature;
            private String synchronous;
            private String asynchronous;

            @CanIgnoreReturnValue
            public Builder source(String source) {
                this.source = source;
                return this;
            }

            @CanIgnoreReturnValue
            public Builder destination(String destination) {
                this.destination = destination;
                return this;
            }

            @CanIgnoreReturnValue
            public Builder signature(String signature) {
                this.signature = signature;
                return this;
            }

            @CanIgnoreReturnValue
            public Builder synchronous(String synchronous) {
                this.synchronous = synchronous;
                return this;
            }

            @CanIgnoreReturnValue
            public Builder asynchronous(String asynchronous) {
                this.asynchronous = asynchronous;
                return this;
            }

            public Key build() {
                return new Key(this);
            }
        }
    }

    public static class Components {
        public static String generateRpcSourceFromCallsite(Callsite callsite) {
            ArrayList<String> rpcSourceElements = new ArrayList<>();
            String rpcSource = "";

            if (getDeiFaultScopeCounterProperty()) {
                rpcSourceElements.add("TestScope" + callsite.getCurrentFaultScope());
            }

            if (getSourceInclude()) {
                rpcSourceElements.add(callsite.getServiceName());
                // TODO: this is super fucking hack please fix
                rpcSource = String.join("+", rpcSourceElements);
            }

            if (getSourceDigest()) {
                return createDigest(rpcSource);
            } else {
                return '[' + rpcSource + ']';
            }
        }

        public static String generateRpcSignatureFromCallsite(Callsite callsite) {
            ArrayList<String> rpcSignatureElements = new ArrayList<>();
            rpcSignatureElements.add(callsite.getClassOrModuleName());
            rpcSignatureElements.add(callsite.getMethodOrFunctionName());
            rpcSignatureElements.add(callsite.getParameterList());
            String rpcSignature = "";

            if (getSignatureInclude()) {
                rpcSignature = String.join(",", rpcSignatureElements);
            }

            if (getSignatureDigest()) {
                return createDigest(rpcSignature);
            } else {
                return '[' + rpcSignature + ']';
            }
        }

        public static String generateRpcSynchronousComponentFromCallsite(Callsite callsite) {
            ArrayList<String> rpcSynchronousElements = new ArrayList<>();
            rpcSynchronousElements.add(callsite.getFileName());
            rpcSynchronousElements.add(callsite.getLineNumber());
            rpcSynchronousElements.add(callsite.getSerializedStackTrace());
            String rpcSynchronous = "";

            if (getSynchronousInclude()) {
                rpcSynchronous = String.join(",", rpcSynchronousElements);
            }

            if (getSynchronousDigest()) {
                return createDigest(rpcSynchronous);
            } else {
                return '[' + rpcSynchronous + ']';
            }
        }

        public static String generateRpcAsynchronousComponentFromCallsite(Callsite callsite) {
            ArrayList<String> rpcAsynchronousElements = new ArrayList<>();
            CallsiteArguments callsiteArguments = callsite.getCallsiteArguments();
            rpcAsynchronousElements.add(callsiteArguments.getStringClass());
            rpcAsynchronousElements.add(callsiteArguments.getToStringResult());
            String rpcAsynchronous = "";

            if (getAsynchronousInclude()) {
                rpcAsynchronous = String.join(",", rpcAsynchronousElements);
            }

            if (getAsynchronousDigest()) {
                return createDigest(rpcAsynchronous);
            } else {
                return '[' + rpcAsynchronous + ']';
            }
        }
    }

    @Override
    public DistributedExecutionIndexKey convertCallsiteToDistributedExecutionIndexKey(Callsite callsite) {
        Key key = new Builder()
                .source(generateRpcSourceFromCallsite(callsite))
                .destination(callsite.getClassOrModuleName())
                .signature(generateRpcSignatureFromCallsite(callsite))
                .synchronous(generateRpcSynchronousComponentFromCallsite(callsite))
                .asynchronous(generateRpcAsynchronousComponentFromCallsite(callsite))
                .build();
        return key;
    }

    @Override
    public String projectionLastKeyWithOnlySignature() {
        Map.Entry<DistributedExecutionIndexKey, Integer> lastCallstackEntry = callstack.get(callstack.size() - 1);
        DistributedExecutionIndexKey key = lastCallstackEntry.getKey();
        return key.onlySignature().toString();
    }

    @Override
    public String projectionLastKeyWithOnlyDestination() {
        Map.Entry<DistributedExecutionIndexKey, Integer> lastCallstackEntry = callstack.get(callstack.size() - 1);
        DistributedExecutionIndexKey key = lastCallstackEntry.getKey();
        return key.onlyDestination().toString();
    }
}
