package cloud.filibuster.instrumentation.libraries.opentelemetry;

import io.opentelemetry.context.ContextKey;

public class OpenTelemetryContextStorageConstants {
  final public static ContextKey<String> VCLOCK_KEY = ContextKey.named("filibuster-vclock");
  final public static ContextKey<String> ORIGIN_VCLOCK_KEY = ContextKey.named("filibuster-origin-vclock");
  final public static ContextKey<String> REQUEST_ID_KEY = ContextKey.named("filibuster-request-id");
  final public static ContextKey<String> EXECUTION_INDEX_KEY = ContextKey.named("filibuster-execution-index");
}
