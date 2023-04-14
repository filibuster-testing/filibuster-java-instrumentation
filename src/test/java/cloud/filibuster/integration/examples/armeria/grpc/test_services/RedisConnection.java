package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisConnection {
    public StatefulRedisConnection<String, String> connection;
    private static RedisConnection single_instance = null;

    private RedisConnection() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
                .withExposedPorts(6379);
        redis.start();
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        RedisClient client = RedisClient.create(String.format("redis://%s:%d/0", address, port));
        connection = client.connect();
    }

    // Static method to create instance of Singleton class
    public static synchronized RedisConnection getInstance()
    {
        if (single_instance == null)
            single_instance = new RedisConnection();
        return single_instance;
    }
}
