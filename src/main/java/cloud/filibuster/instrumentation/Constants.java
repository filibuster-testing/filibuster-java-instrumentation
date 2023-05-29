package cloud.filibuster.instrumentation;

public class Constants {
    final public static String FILIBUSTER_VCLOCK = "_filibuster_vclock";
    final public static String FILIBUSTER_ORIGIN_VCLOCK = "_filibuster_origin_vclock";
    final public static String FILIBUSTER_REQUEST_ID = "_filibuster_request_id";
    final public static String FILIBUSTER_EXECUTION_INDEX = "_filibuster_execution_index";

    // Module names are used in the DB interceptors and analysis configuration files. They are used as
    // classOrModuleName for CallSite objects or for pattern recognition in the analysis configuration files.
    final public static String REDIS_MODULE_NAME = "Lettuce";
    final public static String COCKROACH_MODULE_NAME = "PGSimpleDataSource";
}
