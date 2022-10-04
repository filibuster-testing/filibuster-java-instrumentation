package cloud.filibuster.instrumentation.helpers;

import cloud.filibuster.dei.DistributedExecutionIndexType;

import javax.annotation.Nullable;

import java.util.Objects;

import static cloud.filibuster.dei.DistributedExecutionIndexType.V1;

public class Property {
    private Property() {

    }

    private static boolean isPropertyNull(@Nullable String propertyValue) {
        return Objects.equals(propertyValue, "null") || propertyValue == null;
    }

    /***********************************************************************************
     ** filibuster.dei.version
     ***********************************************************************************/

    private final static String DEI_VERSION = "filibuster.dei.version";

    public static void setDeiVersionProperty(DistributedExecutionIndexType value) {
        System.setProperty(DEI_VERSION, String.valueOf(value));
    }

    public static DistributedExecutionIndexType getDeiVersionProperty() {
        String propertyValue = System.getProperty(DEI_VERSION);

        if (isPropertyNull(propertyValue)) {
            return V1;
        } else {
            return DistributedExecutionIndexType.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.instrumentation.enabled
     ***********************************************************************************/

    private final static String INSTRUMENTATION_ENABLED = "filibuster.instrumentation.enabled";

    public static void setInstrumentationEnabledProperty(boolean value) {
        System.setProperty(INSTRUMENTATION_ENABLED, String.valueOf(value));
    }

    public static boolean getInstrumentationEnabledProperty() {
        String propertyValue = System.getProperty(INSTRUMENTATION_ENABLED);

        if (isPropertyNull(propertyValue)) {
            return false;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.instrumentation.counterexample.file
     ***********************************************************************************/

    private final static String INSTRUMENTATION_COUNTEREXAMPLE_FILE = "filibuster.instrumentation.counterexample.file";

    public static void setInstrumentationCounterexampleFileProperty(String value) {
        System.setProperty(INSTRUMENTATION_COUNTEREXAMPLE_FILE, value);
    }

    @Nullable
    public static String getInstrumentationCounterexampleFileProperty() {
        return System.getProperty(INSTRUMENTATION_COUNTEREXAMPLE_FILE);
    }

    /***********************************************************************************
     ** filibuster.instrumentation.server_communication.enabled
     ***********************************************************************************/

    private final static String INSTRUMENTATION_SERVER_COMMUNICATION_ENABLED = "filibuster.instrumentation.server_communication.enabled";

    public static void setInstrumentationServerCommunicationEnabledProperty(boolean value) {
        System.setProperty(INSTRUMENTATION_SERVER_COMMUNICATION_ENABLED, String.valueOf(value));
    }

    public static boolean getInstrumentationServerCommunicationEnabledProperty() {
        String propertyValue = System.getProperty(INSTRUMENTATION_SERVER_COMMUNICATION_ENABLED);

        if (isPropertyNull(propertyValue)) {
            return true;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.callsite.stack_trace_whitelist
     ***********************************************************************************/

    private final static String CALLSITE_STACK_TRACE_WHITELIST = "filibuster.callsite.stack_trace_whitelist";

    public static void setCallsiteStackTraceWhitelistProperty(String value) {
        System.setProperty(CALLSITE_STACK_TRACE_WHITELIST, value);
    }

    @Nullable
    public static String getCallsiteStackTraceWhitelistProperty() {
        return System.getProperty(CALLSITE_STACK_TRACE_WHITELIST);
    }

    /***********************************************************************************
     ** filibuster.callsite.remove_imports_from_stack_trace
     ***********************************************************************************/

    private final static String CALLSITE_REMOVE_IMPORTS_FROM_STACK_TRACE = "filibuster.callsite.remove_imports_from_stack_trace";

    public static void setCallsiteRemoveImportsFromStackTraceProperty(boolean value) {
        System.setProperty(CALLSITE_REMOVE_IMPORTS_FROM_STACK_TRACE, String.valueOf(value));
    }

    public static boolean getCallsiteRemoveImportsFromStackTraceProperty() {
        String propertyValue = System.getProperty(CALLSITE_REMOVE_IMPORTS_FROM_STACK_TRACE);

        if (isPropertyNull(propertyValue)) {
            return false;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.client_instrumentor.use_override_request_id
     ***********************************************************************************/

    private final static String CLIENT_INSTRUMENTOR_USE_OVERRIDE_REQUEST_ID = "filibuster.client_instrumentor.use_override_request_id";

    public static void setClientInstrumentorUseOverrideRequestIdProperty(boolean value) {
        System.setProperty(CLIENT_INSTRUMENTOR_USE_OVERRIDE_REQUEST_ID, String.valueOf(value));
    }

    public static boolean getClientInstrumentorUseOverrideRequestIdProperty() {
        String propertyValue = System.getProperty(CLIENT_INSTRUMENTOR_USE_OVERRIDE_REQUEST_ID);

        if (isPropertyNull(propertyValue)) {
            return false;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.callsite.line_number
     ***********************************************************************************/

    private final static String CALLSITE_LINE_NUMBER = "filibuster.callsite.line_number";

    public static void setCallsiteLineNumberProperty(boolean value) {
        System.setProperty(CALLSITE_LINE_NUMBER, String.valueOf(value));
    }

    public static boolean getCallsiteLineNumberProperty() {
        String propertyValue = System.getProperty(CALLSITE_LINE_NUMBER);

        if (isPropertyNull(propertyValue)) {
            return true;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.callsite.hash_callsite
     ***********************************************************************************/

    private final static String CALLSITE_HASH_CALLSITE = "filibuster.callsite.hash_callsite";

    public static void setCallsiteHashCallsiteProperty(boolean value) {
        System.setProperty(CALLSITE_HASH_CALLSITE, String.valueOf(value));
    }

    public static boolean getDEIV1HashCallsiteProperty() {
        String propertyValue = System.getProperty(CALLSITE_HASH_CALLSITE);

        if (isPropertyNull(propertyValue)) {
            return true;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.callsite.include_payload
     ***********************************************************************************/

    private final static String CALLSITE_INCLUDE_PAYLOAD = "filibuster.callsite.include_payload";

    public static void setCallsiteIncludePayloadProperty(boolean value) {
        System.setProperty(CALLSITE_INCLUDE_PAYLOAD, String.valueOf(value));
    }

    public static boolean getDEIV1IncludePayloadProperty() {
        String propertyValue = System.getProperty(CALLSITE_INCLUDE_PAYLOAD);

        if (isPropertyNull(propertyValue)) {
            return true;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.callsite.include_stack_trace
     ***********************************************************************************/

    private final static String CALLSITE_INCLUDE_STACK_TRACE = "filibuster.callsite.include_stack_trace";

    public static void setCallsiteIncludeStackTraceProperty(boolean value) {
        System.setProperty(CALLSITE_INCLUDE_STACK_TRACE, String.valueOf(value));
    }

    public static boolean getDEIV1IncludeStackTraceProperty() {
        String propertyValue = System.getProperty(CALLSITE_INCLUDE_STACK_TRACE);

        if (isPropertyNull(propertyValue)) {
            return true;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }
}
