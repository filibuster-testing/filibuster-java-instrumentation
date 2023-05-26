package cloud.filibuster.instrumentation.libraries.cockroachdb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CockroachInterceptor<T> implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(CockroachInterceptor.class.getName());
    private final T targetObject;

    @SuppressWarnings("unchecked")
    public static <T> T createInterceptor(T target, Class<T> itfc) {
        logger.log(Level.INFO, "CockroachInterceptor: constructor");
        return (T) Proxy.newProxyInstance(
                itfc.getClassLoader(),
                new Class<?>[]{itfc},
                new CockroachInterceptor<>(target)
        );
    }

    private CockroachInterceptor(T targetObject) {
        this.targetObject = targetObject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.log(Level.INFO, "CockroachInterceptor: Invocation was intercepted");
//        return method.invoke(targetObject, args);
        return "InterceptedData";
    }

}
