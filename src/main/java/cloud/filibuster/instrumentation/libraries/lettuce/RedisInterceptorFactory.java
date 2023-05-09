package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


public final class RedisInterceptorFactory {
    private final RedisClient redisClient;
    private final String redisConnectionString;

    public RedisInterceptorFactory() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);
        redis.start();
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        redisConnectionString = String.format("redis://%s:%d/0", address, port);
        redisClient = RedisClient.create(redisConnectionString);
    }

    public RedisInterceptorFactory(RedisClient redisClient, String redisConnectionString) {
        this.redisClient = redisClient;
        this.redisConnectionString = redisConnectionString;
    }

    public <T> T getProxy(Class<T> commandType) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(commandType);
        myFactory.addInterceptor(new RedisClientInterceptor<>(redisClient, redisConnectionString, commandType));
        return myFactory.createProxy(commandType.getClassLoader());
    }
}
