package cloud.filibuster.functional.java.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class JUnitRedisAccessTest {

    private RedisBackedCache underTest;

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
        underTest = new RedisBackedCache(address, port);
    }

    @Test
    public void testSimplePutAndGet() {
        underTest.put("test", "example");

        String retrieved = underTest.get("test");
        assertEquals(retrieved, "example");
    }
}
