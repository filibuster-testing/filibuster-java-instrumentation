package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;

public class RedisExecutionInterceptor implements MethodInterceptor {
    private final StatefulRedisConnection<String, String> redisConnection;
    private final String redisConnectionString;

    public RedisExecutionInterceptor(StatefulRedisConnection<String, String> redisConnection, String redisConnectionString) {
        this.redisConnection = redisConnection;
        this.redisConnectionString = redisConnectionString;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?>[] paramTypes = new Class[invocation.getMethod().getParameterCount()];
        Arrays.fill(paramTypes, Object.class);
        Method method = redisConnection.sync().getClass().getDeclaredMethod(invocation.getMethod().getName(),
                paramTypes);
        Object result = method.invoke(redisConnection.sync(), invocation.getArguments());
        // Communicate result with the Filibuster server
        return result;
    }
}
