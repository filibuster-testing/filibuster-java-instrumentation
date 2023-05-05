package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class FilibusterRedisClientInterceptor {
    private final StatefulRedisConnection<String, String> redisConnection;
    private final String redisConnectionString;

    public FilibusterRedisClientInterceptor() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
                .withExposedPorts(6379);
        redis.start();
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        redisConnectionString = String.format("redis://%s:%d/0", address, port);
        RedisClient client = RedisClient.create(redisConnectionString);
        redisConnection = client.connect();
    }

    public FilibusterRedisClientInterceptor(RedisClient redisClient, String redisConnectionString) {
        this.redisConnection = redisClient.connect();
        this.redisConnectionString = redisConnectionString;
    }

    private  <T> T createProxy(StatefulRedisConnection<String, String> redisConnection, Class<T> type) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(type);
        myFactory.addInterceptor(new RedisIntermediaryInterceptor(redisConnection, redisConnectionString));
        myFactory.addInterceptor(new RedisExecutionInterceptor(redisConnection, redisConnectionString));
        return myFactory.createProxy(type.getClassLoader());
    }

    public  <T> T getConnection(Class<T> type, boolean isFaultInjected) {
        if (isFaultInjected) {
            RedisIntermediaryInterceptor.isFaultInjected = true;
            return createProxy(redisConnection, type);
        }
        if (type == RedisCommands.class)
            return type.cast(redisConnection.sync());
        if (type == RedisAsyncCommands.class)
            return type.cast(redisConnection.async());
        if (type == RedisReactiveCommands.class)
            return type.cast(redisConnection.reactive());
        throw new IllegalArgumentException("Unknown type");
    }
}
