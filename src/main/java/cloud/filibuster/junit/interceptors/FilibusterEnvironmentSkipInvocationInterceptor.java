package cloud.filibuster.junit.interceptors;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;

/**
 * Invocation interceptor that can be used to conditionally prohibit a test from running in GitHub Actions.
 *
 * Used when test requires dependencies that are currently unavailable: for example, the Filibuster Python service.
 */
public class FilibusterEnvironmentSkipInvocationInterceptor implements InvocationInterceptor {
    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext,
                                          ExtensionContext extensionContext) throws Throwable {
        if (System.getenv("FILIBUSTER_DISABLED") != null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext,
                                         ExtensionContext extensionContext) throws Throwable {
        if (System.getenv("FILIBUSTER_DISABLED") != null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {
        if (System.getenv("FILIBUSTER_DISABLED") != null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        if (System.getenv("FILIBUSTER_DISABLED") != null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }
}
