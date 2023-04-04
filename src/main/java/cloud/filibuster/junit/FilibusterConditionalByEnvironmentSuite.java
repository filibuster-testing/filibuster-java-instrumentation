package cloud.filibuster.junit;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use of this annotation avoids running this entire class if the annotation is set and the FILIBUSTER_DISABLED parameter is set.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@DisabledIfEnvironmentVariable(named = "FILIBUSTER_DISABLED", matches = "true")
public @interface FilibusterConditionalByEnvironmentSuite {
}
