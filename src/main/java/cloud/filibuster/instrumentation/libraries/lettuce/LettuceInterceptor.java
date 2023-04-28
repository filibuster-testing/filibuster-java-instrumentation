package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;



public class LettuceInterceptor implements MethodInterceptor {

    private final StatefulRedisConnection<String, String> redisConnection; //Will be needed later when data failures are injected
    public static boolean isFaultInjected = false;

    public LettuceInterceptor(StatefulRedisConnection<String, String> redisConnection) {
        this.redisConnection = redisConnection;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (isFaultInjected) {
            throw new Exception("An exception was thrown at LettuceInterceptor");
        }
        return invocation.proceed();
    }
}
