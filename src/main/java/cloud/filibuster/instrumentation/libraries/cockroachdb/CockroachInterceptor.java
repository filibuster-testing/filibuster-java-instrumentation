package cloud.filibuster.instrumentation.libraries.cockroachdb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CockroachInterceptor<T> implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(CockroachInterceptor.class.getName());
    private final T targetObject;

    private CockroachInterceptor(T targetObject) {
        logger.log(Level.INFO, "CockroachInterceptor: Constructor was called");
        this.targetObject = targetObject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.log(Level.INFO, "CockroachInterceptor: Invocation was intercepted");
        Object invocationResult = method.invoke(targetObject, args);
        logger.log(Level.INFO, "CockroachInterceptor: Invocation result was returned: " + invocationResult);
        if (invocationResult.getClass().isInterface()) {
            return createInterceptor(invocationResult);
        }
        return invocationResult;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createInterceptor(T target) {
        logger.log(Level.INFO, "CockroachInterceptor: createInterceptor was called");
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new CockroachInterceptor<>(target)
        );
    }

}
