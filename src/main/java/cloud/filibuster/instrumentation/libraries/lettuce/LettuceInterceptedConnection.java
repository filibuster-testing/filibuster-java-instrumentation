package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;

public abstract class LettuceInterceptedConnection {

    public static RedisCommands<String, String> create (StatefulRedisConnection<String, String> redisConnection) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(RedisCommands.class);
        myFactory.addInterceptor(new LettuceInterceptor(redisConnection));
        myFactory.addInterceptor(new LettuceInterceptorExecutor(redisConnection));
        return myFactory.createProxy(RedisCommands.class.getClassLoader());
    }
}
