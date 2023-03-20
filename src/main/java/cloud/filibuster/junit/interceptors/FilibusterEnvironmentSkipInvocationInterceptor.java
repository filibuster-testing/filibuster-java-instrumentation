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

    public void conditionalInvocationOnFilibusterEnvironment(InvocationInterceptor.Invocation<Void> invocation) throws Throwable {
        if (System.getenv("FILIBUSTER_DISABLED") != null) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptAfterAllMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        conditionalInvocationOnFilibusterEnvironment(invocation);
    }

    @Override
    public void interceptAfterEachMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        conditionalInvocationOnFilibusterEnvironment(invocation);
    }

    @Override
    public void interceptBeforeAllMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        conditionalInvocationOnFilibusterEnvironment(invocation);
    }

    @Override
    public void interceptBeforeEachMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        conditionalInvocationOnFilibusterEnvironment(invocation);
    }

//    public void interceptDynamicTest(InvocationInterceptor.Invocation<Void> invocation, DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext) throws Throwable {
//        conditionalInvocationOnFilibusterEnvironment(invocation);
//    }
//
//    public void interceptTestClassConstructor(InvocationInterceptor.Invocation<T> invocation, ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) throws Throwable {
//        conditionalInvocationOnFilibusterEnvironment(invocation);
//    }
//
//    public void interceptTestFactoryMethod(InvocationInterceptor.Invocation<T> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
//        conditionalInvocationOnFilibusterEnvironment(invocation);
//    }

    @Override
    public void interceptTestMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        conditionalInvocationOnFilibusterEnvironment(invocation);
    }

    @Override
    public void interceptTestTemplateMethod(InvocationInterceptor.Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        conditionalInvocationOnFilibusterEnvironment(invocation);
    }

}
