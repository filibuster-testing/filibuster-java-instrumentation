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
     ** filibuster.server.backend.can_invoke_directly
     ***********************************************************************************/

    private final static String SERVER_BACKEND_CAN_INVOKE_DIRECTLY = "filibuster.server.backend.can_invoke_directly";

    public final static boolean SERVER_BACKEND_CAN_INVOKE_DIRECTLY_DEFAULT = false;

    public static void setServerBackendCanInvokeDirectlyProperty(boolean canInvokeDirectly) {
        System.setProperty(SERVER_BACKEND_CAN_INVOKE_DIRECTLY, String.valueOf(canInvokeDirectly));
    }

    public static boolean getServerBackendCanInvokeDirectlyProperty() {
        String propertyValue = System.getProperty(SERVER_BACKEND_CAN_INVOKE_DIRECTLY);

        if (isPropertyNull(propertyValue)) {
            return SERVER_BACKEND_CAN_INVOKE_DIRECTLY_DEFAULT;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.server.backend.docker_image
     ***********************************************************************************/

    private final static String SERVER_BACKEND_DOCKER_IMAGE_NAME = "filibuster.server.backend.docker_image_name";

    public final static String SERVER_BACKEND_DOCKER_IMAGE_NAME_DEFAULT = "filibustertesting/filibuster:0.34";

    public static void setServerBackendDockerImageNameProperty(String dockerImageName) {
        System.setProperty(SERVER_BACKEND_DOCKER_IMAGE_NAME, dockerImageName);
    }

    public static String getServerBackendDockerImageNameProperty() {
        String propertyValue = System.getProperty(SERVER_BACKEND_DOCKER_IMAGE_NAME);

        if (isPropertyNull(propertyValue)) {
            return SERVER_BACKEND_DOCKER_IMAGE_NAME_DEFAULT;
        } else {
            return propertyValue;
        }
    }

    /***********************************************************************************
     ** filibuster.server.port
     ***********************************************************************************/

    private final static String SERVER_PORT = "filibuster.server.port";

    public final static int SERVER_PORT_DEFAULT = 5005;

    public static void setServerPortProperty(int port) {
        System.setProperty(SERVER_PORT, String.valueOf(port));
    }

    public static int getServerPortProperty() {
        String propertyValue = System.getProperty(SERVER_PORT);

        if (isPropertyNull(propertyValue)) {
            return SERVER_PORT_DEFAULT;
        } else {
            return Integer.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.server.host
     ***********************************************************************************/

    private final static String SERVER_HOST = "filibuster.server.host";

    public final static String SERVER_HOST_DEFAULT = "localhost";

    public static void setServerHostProperty(String host) {
        System.setProperty(SERVER_HOST, host);
    }

    public static String getServerHostProperty() {
        String propertyValue = System.getProperty(SERVER_HOST);

        if (isPropertyNull(propertyValue)) {
            return SERVER_HOST_DEFAULT;
        } else {
            return propertyValue;
        }
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
     ** filibuster.enabled
     ***********************************************************************************/

    private final static String ENABLED = "filibuster.enabled";

    public static void setEnabledProperty(boolean value) {
        System.setProperty(ENABLED, String.valueOf(value));
    }

    public static boolean getEnabledProperty() {
        String propertyValue = System.getProperty(ENABLED);

        if (isPropertyNull(propertyValue)) {
            return false;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.test.avoid_redundant_injections
     ***********************************************************************************/

    public static final boolean AVOID_REDUNDANT_INJECTIONS_DEFAULT = false;

    private final static String TEST_AVOID_REDUNDANT_INJECTIONS = "filibuster.test.avoid_redundant_injections";

    public static void setTestAvoidRedundantInjectionsProperty(boolean value) {
        System.setProperty(TEST_AVOID_REDUNDANT_INJECTIONS, String.valueOf(value));
    }

    public static boolean getTestAvoidRedundantInjectionsProperty() {
        String propertyValue = System.getProperty(TEST_AVOID_REDUNDANT_INJECTIONS);

        if (isPropertyNull(propertyValue)) {
            return AVOID_REDUNDANT_INJECTIONS_DEFAULT;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.test.avoid_injections_on_organic_failures
     ***********************************************************************************/

    public static final boolean AVOID_INJECTIONS_ON_ORGANIC_FAILURES_DEFAULT = false;

    private final static String TEST_AVOID_INJECTIONS_ON_ORGANIC_FAILURES = "filibuster.test.avoid_injections_on_organic_failures";

    public static void setTestAvoidInjectionsOnOrganicFailuresProperty(boolean value) {
        System.setProperty(TEST_AVOID_INJECTIONS_ON_ORGANIC_FAILURES, String.valueOf(value));
    }

    public static boolean getTestAvoidInjectionsOnOrganicFailuresProperty() {
        String propertyValue = System.getProperty(TEST_AVOID_INJECTIONS_ON_ORGANIC_FAILURES);

        if (isPropertyNull(propertyValue)) {
            return AVOID_INJECTIONS_ON_ORGANIC_FAILURES_DEFAULT;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.test.data_nondeterminism
     ***********************************************************************************/

    public static final boolean DATA_NONDETERMINISM_DEFAULT = false;

    private final static String TEST_DATA_NONDETERMINISM = "filibuster.test.data_nondeterminism";

    public static void setTestDataNondeterminismProperty(boolean value) {
        System.setProperty(TEST_DATA_NONDETERMINISM, String.valueOf(value));
    }

    public static boolean getTestDataNondeterminismProperty() {
        String propertyValue = System.getProperty(TEST_DATA_NONDETERMINISM);

        if (isPropertyNull(propertyValue)) {
            return DATA_NONDETERMINISM_DEFAULT;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.test.max_iteratons
     ***********************************************************************************/

    public static final int MAX_ITERATIONS_DEFAULT = 99;

    private final static String TEST_MAX_ITERATIONS = "filibuster.test.max_iterations";

    public static void setTestMaxIterationsProperty(int value) {
        System.setProperty(TEST_MAX_ITERATIONS, String.valueOf(value));
    }

    public static int getTestMaxIterationsProperty() {
        String propertyValue = System.getProperty(TEST_MAX_ITERATIONS);

        if (isPropertyNull(propertyValue)) {
            return MAX_ITERATIONS_DEFAULT;
        } else {
            return Integer.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.test.suppress_combinations
     ***********************************************************************************/

    public static final boolean SUPPRESS_COMBINATIONS_DEFAULT = false;

    private final static String TEST_SUPPRESS_COMBINATIONS = "filibuster.test.suppress_combinations";

    public static void setTestSuppressCombinationsProperty(boolean value) {
        System.setProperty(TEST_SUPPRESS_COMBINATIONS, String.valueOf(value));
    }

    public static boolean getTestSuppressCombinationsProperty() {
        String propertyValue = System.getProperty(TEST_SUPPRESS_COMBINATIONS);

        if (isPropertyNull(propertyValue)) {
            return SUPPRESS_COMBINATIONS_DEFAULT;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /***********************************************************************************
     ** filibuster.test.analysis_resource_file
     ***********************************************************************************/

    private final static String TEST_ANALYSIS_RESOURCE_FILE = "filibuster.test.analysis_resource_file";

    public static void setTestAnalysisResourceFileProperty(String value) {
        System.setProperty(TEST_ANALYSIS_RESOURCE_FILE, String.valueOf(value));
    }

    public static String getTestAnalysisResourceFileProperty() {
        String propertyValue = System.getProperty(TEST_ANALYSIS_RESOURCE_FILE);

        if (isPropertyNull(propertyValue)) {
            return "";
        } else {
            return propertyValue;
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
     ** filibuster.reports.test_suite_report.enabled
     ***********************************************************************************/

    private final static String REPORTS_TEST_SUITE_REPORT_ENABLED = "filibuster.reports.test_suite_report.enabled";

    public static void setReportsTestSuiteReportEnabledProperty(boolean value) {
        System.setProperty(REPORTS_TEST_SUITE_REPORT_ENABLED, String.valueOf(value));
    }

    public static boolean getReportsTestSuiteReportEnabledProperty() {
        String propertyValue = System.getProperty(REPORTS_TEST_SUITE_REPORT_ENABLED);
        if (isPropertyNull(propertyValue)) {
            return true;
        } else {
            return Boolean.parseBoolean(propertyValue);
        }
    }


}
