package cloud.filibuster.instrumentation.helpers;

import java.lang.reflect.Field;
import java.util.Map;

public class Environment {
    private Environment() {

    }

    // Taken from: https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
    @SuppressWarnings("unchecked")
    public static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

    // Adapted from: https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
    @SuppressWarnings("unchecked")
    public static void unsetEnv(String key) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.remove(key);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }
}
