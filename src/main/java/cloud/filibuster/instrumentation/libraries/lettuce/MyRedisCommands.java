package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.dynamic.Commands;

public interface MyRedisCommands extends Commands {
    String get(String key);
    String set(String key, String value);
}