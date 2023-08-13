package cloud.filibuster.functional.database.redis;

import cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.RedisClientService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static cloud.filibuster.instrumentation.helpers.Property.setRedisTestPortNondeterminismProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisPortNonDeterminismJUnitTest {
    static String connectionString = RedisClientService.getInstance().connectionString;
    static Constructor<?> dynamicProxyConstructor;
    static Field serviceName;

    @BeforeAll
    public static void getDynamicProxyConstruction() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        Class<?> dynamicProxyClass = Class.forName("cloud.filibuster.instrumentation.libraries.dynamic.proxy.DynamicProxyInterceptor");
        serviceName = dynamicProxyClass.getDeclaredField("serviceName");
        serviceName.setAccessible(true);
        dynamicProxyConstructor = dynamicProxyClass.getDeclaredConstructor(Object.class, String.class);
        dynamicProxyConstructor.setAccessible(true);
    }

    @Test
    @Order(1)
    public void testRedisPortNonDeterminismTrue() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        setRedisTestPortNondeterminismProperty(true);
        DynamicProxyInterceptor<?> redisInterceptor = (DynamicProxyInterceptor<?>) dynamicProxyConstructor.newInstance(new Object(), connectionString);
        assertEquals("localhost", serviceName.get(redisInterceptor));
    }

    @Test
    @Order(2)
    public void testRedisPortNonDeterminismFalse() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        setRedisTestPortNondeterminismProperty(false);
        DynamicProxyInterceptor<?> redisInterceptor = (DynamicProxyInterceptor<?>) dynamicProxyConstructor.newInstance(new Object(), connectionString);
        assertEquals(connectionString, serviceName.get(redisInterceptor));
    }

}
