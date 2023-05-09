package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.dynamic.intercept.InvocationProxyFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("unchecked")
public final class RedisInterceptorFactory <T> {
    private final T objectToIntercept;
    private final String redisConnectionString;

    public RedisInterceptorFactory() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);
        redis.start();
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        redisConnectionString = String.format("redis://%s:%d/0", address, port);
        objectToIntercept = (T) RedisClient.create(redisConnectionString).connect();
    }

    public RedisInterceptorFactory(T objectToIntercept, String redisConnectionString) {
        this.objectToIntercept = objectToIntercept;
        this.redisConnectionString = redisConnectionString;
    }

    public <L> L getProxy(Class<L> itfc) {
        InvocationProxyFactory myFactory = new InvocationProxyFactory();
        myFactory.addInterface(itfc);
        myFactory.addInterceptor(new RedisInterceptor<>(objectToIntercept, redisConnectionString));
        return myFactory.createProxy(itfc.getClassLoader());
    }
}
