package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;

public class RedisExecutionInterceptor implements MethodInterceptor {
    private final StatefulRedisConnection<String, String> redisConnection;

    public RedisExecutionInterceptor(StatefulRedisConnection<String, String> redisConnection) {
        this.redisConnection = redisConnection;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?>[] paramTypes = new Class[invocation.getMethod().getParameterCount()];
        Arrays.fill(paramTypes, Object.class);
        Method method = redisConnection.sync().getClass().getDeclaredMethod(invocation.getMethod().getName(),
                paramTypes);
        return method.invoke(redisConnection.sync(), invocation.getArguments());
    }
}
