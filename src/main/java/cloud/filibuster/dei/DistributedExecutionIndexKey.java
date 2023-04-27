package cloud.filibuster.dei;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;

public interface DistributedExecutionIndexKey {
    String serialize();

    @Override String toString();

    @Override int hashCode();

    @Override boolean equals(Object o);

    String onlyMetadataAndSignature();

    String onlySignature();

    String onlyDestination();

    static DistributedExecutionIndexKey deserialize(String serialized) {
        String[] stringArray = serialized.split("-", 6);

        // Assuming only a single version right now.
        return new DistributedExecutionIndexV1.Key.Builder()
                .metadata(stringArray[1])
                .source(stringArray[2])
                .signature(stringArray[3])
                .synchronous(stringArray[4])
                .asynchronous(stringArray[5])
                .build();
    }
}
