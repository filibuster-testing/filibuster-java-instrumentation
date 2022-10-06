package cloud.filibuster.dei.implementations;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexBase;
import cloud.filibuster.dei.DistributedExecutionIndexKey;
import cloud.filibuster.dei.DistributedExecutionIndexType;
import cloud.filibuster.instrumentation.datatypes.Callsite;

import java.util.ArrayList;
import java.util.Objects;

import static cloud.filibuster.dei.DistributedExecutionIndexType.V1;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Asynchronous.getAsynchronousDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Asynchronous.getAsynchronousInclude;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Signature.getSignatureDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Signature.getSignatureInclude;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Source.getSourceDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Source.getSourceInclude;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Synchronous.getSynchronousDigest;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.Synchronous.getSynchronousInclude;

import static cloud.filibuster.instrumentation.helpers.Hashing.createDigest;

public class DistributedExecutionIndexV1 extends DistributedExecutionIndexBase implements DistributedExecutionIndex {
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
        private final DistributedExecutionIndexType version;
        private final String rpcSource;
        private final String rpcSignature;
        private final String rpcSynchronous;
        private final String rpcAsynchronous;

        public Key(Builder builder) {
            this.version = builder.version;
            this.rpcSource = builder.rpcSource;
            this.rpcSignature = builder.rpcSignature;
            this.rpcSynchronous = builder.rpcSynchronous;
            this.rpcAsynchronous = builder.rpcAsynchronous;
        }

        public static class Builder {
            private DistributedExecutionIndexType version = V1;
            private String rpcSource;
            private String rpcSignature;
            private String rpcSynchronous;
            private String rpcAsynchronous;

            public Builder rpcSourceService(String rpcSource) {
                this.rpcSource = rpcSource;
                return this;
            }

            public Builder rpcSignature(String rpcSignature) {
                this.rpcSignature = rpcSignature;
                return this;
            }

            public Builder rpcSynchronous(String rpcSynchronous) {
                this.rpcSynchronous = rpcSynchronous;
                return this;
            }

            public Builder rpcAsynchronous(String rpcAsynchronous) {
                this.rpcAsynchronous = rpcAsynchronous;
                return this;
            }

            public Key build() {
                return new Key(this);
            }
        }
    }

    public static final DistributedExecutionIndexType VERSION = V1;

    public String generateRpcSourceFromCallsite(Callsite callsite) {
        String rpcSource = "";

        if (getSourceInclude()) {
            rpcSource = callsite.getServiceName();
        }

        if (getSourceDigest()) {
            return createDigest(rpcSource);
        } else {
            return '[' + rpcSource + ']';
        }
    }

    public String generateRpcSignatureFromCallsite(Callsite callsite) {
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

    public String generateRpcSynchronousComponentFromCallsite(Callsite callsite) {
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

    public String generateRpcAsynchronousComponentFromCallsite(Callsite callsite) {
        ArrayList<String> rpcAsynchronousElements = new ArrayList<>();
        rpcAsynchronousElements.add(callsite.getSerializedArguments());
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

    @Override
    public String serializeCallsite(Callsite callsite) {
        ArrayList<String> deiElements = new ArrayList<>();
        deiElements.add(VERSION.name());
        deiElements.add(generateRpcSourceFromCallsite(callsite));
        deiElements.add(generateRpcSignatureFromCallsite(callsite));
        deiElements.add(generateRpcSynchronousComponentFromCallsite(callsite));
        deiElements.add(generateRpcAsynchronousComponentFromCallsite(callsite));
        String result = String.join("-", deiElements);
        return result;
    }
}
