package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;

public abstract class LettuceInterceptedConnection {

    public static <T> T create (StatefulRedisConnection<String, String> redisConnection, Class<T> type) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(type);
        myFactory.addInterceptor(new LettuceInterceptor(redisConnection));
        myFactory.addInterceptor(new LettuceInterceptorExecutor(redisConnection));
        return myFactory.createProxy(type.getClassLoader());
    }
}
