package cloud.filibuster.dei;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;

public interface DistributedExecutionIndexKey {
    String serialize();

    @Override String toString();

    @Override int hashCode();

    @Override boolean equals(Object o);

    static DistributedExecutionIndexKey deserialize(String serialized) {
        String[] stringArray = serialized.split("-", 5);

        // Assuming only a single version right now.
        return new DistributedExecutionIndexV1.Key.Builder()
                .source(stringArray[1])
                .signature(stringArray[2])
                .synchronous(stringArray[3])
                .asynchronous(stringArray[4])
                .build();
    }
}
