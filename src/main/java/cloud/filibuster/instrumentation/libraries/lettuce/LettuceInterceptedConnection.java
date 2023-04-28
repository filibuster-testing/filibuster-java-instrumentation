package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.DefaultMethodInvokingInterceptor;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;

public class LettuceInterceptedConnection {

    public static MyRedisCommands create (StatefulRedisConnection<String, String> redisConnection) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(MyRedisCommands.class);
        myFactory.addInterceptor(new DefaultMethodInvokingInterceptor());
        myFactory.addInterceptor(new LettuceInterceptor(redisConnection));
        myFactory.addInterceptor(new LettuceInterceptorExecutor(redisConnection));
        return myFactory.createProxy(MyRedisCommands.class.getClassLoader());
    }
}
