package cloud.filibuster.dei.implementations;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexBase;
import cloud.filibuster.instrumentation.datatypes.Callsite;

import java.util.ArrayList;
import java.util.Objects;

import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.getHashProperty;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.getPayloadHashProperty;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.getPayloadIncludeProperty;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.getStackTraceHashProperty;
import static cloud.filibuster.dei.implementations.DistributedExecutionIndexV1.Properties.getStackTraceIncludeProperty;
import static cloud.filibuster.instrumentation.helpers.Hashing.createDigest;

public class DistributedExecutionIndexV1 extends DistributedExecutionIndexBase implements DistributedExecutionIndex {
    public static class Properties {
        /***********************************************************************************
         ** filibuster.dei.v1.stack_trace.include
         ***********************************************************************************/

        private final static String STACK_TRACE_INCLUDE = "filibuster.dei.v1.stack_trace.include";

        public static void setStackTraceIncludeProperty(boolean value) {
            System.setProperty(STACK_TRACE_INCLUDE, String.valueOf(value));
        }

        public static boolean getStackTraceIncludeProperty() {
            String propertyValue = System.getProperty(STACK_TRACE_INCLUDE);

            if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                return true;
            } else {
                return Boolean.parseBoolean(propertyValue);
            }
        }

        /***********************************************************************************
         ** filibuster.dei.v1.stack_trace.hash
         ***********************************************************************************/

        private final static String STACK_TRACE_HASH = "filibuster.dei.v1.stack_trace.hash";

        public static void setStackTraceHashProperty(boolean value) {
            System.setProperty(STACK_TRACE_HASH, String.valueOf(value));
        }

        public static boolean getStackTraceHashProperty() {
            String propertyValue = System.getProperty(STACK_TRACE_HASH);

            if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                return true;
            } else {
                return Boolean.parseBoolean(propertyValue);
            }
        }

        /***********************************************************************************
         ** filibuster.dei.v1.payload.include
         ***********************************************************************************/

        private final static String PAYLOAD_INCLUDE = "filibuster.dei.v1.payload.include";

        public static void setPayloadIncludeProperty(boolean value) {
            System.setProperty(PAYLOAD_INCLUDE, String.valueOf(value));
        }

        public static boolean getPayloadIncludeProperty() {
            String propertyValue = System.getProperty(PAYLOAD_INCLUDE);

            if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                return true;
            } else {
                return Boolean.parseBoolean(propertyValue);
            }
        }

        /***********************************************************************************
         ** filibuster.dei.v1.payload.hash
         ***********************************************************************************/

        private final static String PAYLOAD_HASH = "filibuster.dei.v1.payload.hash";

        public static void setPayloadHashProperty(boolean value) {
            System.setProperty(PAYLOAD_HASH, String.valueOf(value));
        }

        public static boolean getPayloadHashProperty() {
            String propertyValue = System.getProperty(PAYLOAD_HASH);

            if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                return true;
            } else {
                return Boolean.parseBoolean(propertyValue);
            }
        }

        /***********************************************************************************
         ** filibuster.dei.v1.hash
         ***********************************************************************************/

        private final static String HASH = "filibuster.dei.v1.hash";

        public static void setHashProperty(boolean value) {
            System.setProperty(HASH, String.valueOf(value));
        }

        public static boolean getHashProperty() {
            String propertyValue = System.getProperty(HASH);

            if (Objects.equals(propertyValue, "null") || propertyValue == null) {
                return true;
            } else {
                return Boolean.parseBoolean(propertyValue);
            }
        }
    }

    @Override
    public void push(Callsite callsite) {
        ArrayList<String> toStringResult = new ArrayList<>();

        toStringResult.add(callsite.getServiceName());
        toStringResult.add(callsite.getFileName());
        toStringResult.add(callsite.getLineNumber());
        toStringResult.add(callsite.getClassOrModuleName());
        toStringResult.add(callsite.getMethodOrFunctionName());

        if (getPayloadIncludeProperty()) {
            if (getPayloadHashProperty()) {
                toStringResult.add(createDigest(callsite.getSerializedArguments()));
            } else {
                toStringResult.add(callsite.getSerializedArguments());
            }
        } else {
            toStringResult.add(createDigest(""));
        }

        if (getStackTraceIncludeProperty()) {
            if (getStackTraceHashProperty()) {
                toStringResult.add(createDigest(callsite.getSerializedStackTrace()));
            } else {
                toStringResult.add(callsite.getSerializedStackTrace());
            }
        } else {
            toStringResult.add(createDigest(""));
        }

        String result = String.join("-", toStringResult);

        if (getHashProperty()) {
            result = createDigest(result);
        }

        push(result);
    }
}
