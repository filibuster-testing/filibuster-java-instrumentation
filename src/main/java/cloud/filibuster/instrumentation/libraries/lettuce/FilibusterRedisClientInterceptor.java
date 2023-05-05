package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
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

    public  <T> T getConnection(Class<T> type) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(type);
        myFactory.addInterceptor(new RedisIntermediaryInterceptor(redisConnection, redisConnectionString));
        return myFactory.createProxy(type.getClassLoader());
    }
}
