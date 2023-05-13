package cloud.filibuster.functional.java.properties;

import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static cloud.filibuster.instrumentation.helpers.Property.setRedisTestPortNondeterminismProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisPortNonDeterminismJUnitTest {
    String redisConnectionString = "redis://localhost:12345/0";

    @Test
    @Order(1)
    public void testRedisPortNonDeterminismTrue() {
        setRedisTestPortNondeterminismProperty(true);
        RedisInterceptor<Object> redisInterceptor = new RedisInterceptor<>(new Object(), redisConnectionString);
        assertEquals("localhost", redisInterceptor.getRedisServiceName());
    }

    @Test
    @Order(2)
    public void testRedisPortNonDeterminismFalse() {
        setRedisTestPortNondeterminismProperty(false);
        RedisInterceptor<Object> redisInterceptor = new RedisInterceptor<>(new Object(), redisConnectionString);
        assertEquals(redisConnectionString, redisInterceptor.getRedisServiceName());
    }

}
