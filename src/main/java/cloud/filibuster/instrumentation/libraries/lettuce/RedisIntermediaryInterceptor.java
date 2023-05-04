package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;



public class RedisIntermediaryInterceptor implements MethodInterceptor {

    private final StatefulRedisConnection<String, String> redisConnection; //Will be needed later when data failures are injected
    public static boolean isFaultInjected = false;

    public RedisIntermediaryInterceptor(StatefulRedisConnection<String, String> redisConnection) {
        this.redisConnection = redisConnection;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (isFaultInjected) {
            throw new RedisCommandTimeoutException("An exception was thrown at LettuceInterceptor");
        }
        return invocation.proceed();
    }
}
