package cloud.filibuster.functional.java.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JUnitRedisAccessTest {

    @DisplayName("Verify connection with Redis database")
    @Test
    @Order(1)
    public void testRedisConnection() {
        RedisClient redisClient = RedisClient.create("redis://password@localhost:6379/0");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String, String> syncCommands = connection.sync();

        syncCommands.set("key", "Hello, Redis!");
        assertEquals(syncCommands.get("key"), "Hello, Redis!");
        connection.close();
        redisClient.shutdown();
    }
}
