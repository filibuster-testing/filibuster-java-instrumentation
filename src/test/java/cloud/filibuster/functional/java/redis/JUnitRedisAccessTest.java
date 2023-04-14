package cloud.filibuster.functional.java.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class JUnitRedisAccessTest {

    private StatefulRedisConnection<String, String> connection;

    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine"))
            .withExposedPorts(6379);

    @BeforeAll
    public static void setUp() {
        redis.start();
    }
    @BeforeEach
    public void setUpContainer() {
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();
        RedisClient client = RedisClient.create(String.format("redis://%s:%d/0", address, port));
        connection = client.connect();
    }

    @Test
    public void testSimplePutAndGet() {
        connection.sync().set("test", "example");

        String retrieved = connection.sync().get("test");
        assertEquals(retrieved, "example");
    }
}
