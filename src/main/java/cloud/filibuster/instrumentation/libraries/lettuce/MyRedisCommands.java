package cloud.filibuster.instrumentation.libraries.lettuce;

import io.lettuce.core.dynamic.Commands;

public interface MyRedisCommands extends Commands {
    String get(String key); // Synchronous Execution of GET
    String set(String key, String value); // asynchronous SET execution
}