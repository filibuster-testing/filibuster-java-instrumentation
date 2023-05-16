package cloud.filibuster.integration.examples.armeria.grpc.test_services;

import io.lettuce.core.RedisClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nullable;

public class RedisClientService {
    public RedisClient redisClient;
    public String connectionString;

    @Nullable
    private static RedisClientService single_instance = null;

    @SuppressWarnings("resource")
    private RedisClientService() {
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
                .withExposedPorts(6379);
        redis.start();
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        connectionString = String.format("redis://%s:%d/0", address, port);
        redisClient = RedisClient.create(connectionString);
    }

    // Static method to create instance of Singleton class
    public static synchronized RedisClientService getInstance()
    {
        if (single_instance == null) {
            single_instance = new RedisClientService();
        }

        return single_instance;
    }
}
