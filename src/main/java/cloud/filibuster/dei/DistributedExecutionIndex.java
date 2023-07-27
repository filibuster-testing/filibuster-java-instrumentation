package cloud.filibuster.dei;

import cloud.filibuster.instrumentation.datatypes.Callsite;

public interface DistributedExecutionIndex extends Cloneable, Comparable<DistributedExecutionIndex> {

    DistributedExecutionIndexKey convertCallsiteToDistributedExecutionIndexKey(Callsite callsite);

    void push(Callsite callsite);

    void pop();

    DistributedExecutionIndex deserialize(String serialized);

    Object clone();

    String projectionLastKeyWithOnlyMetadataAndSignature();

    String projectionLastKeyWithOnlyMetadata();

    String projectionLastKeyWithOnlySignature();

    String projectionLastKeyWithOnlyDestination();

}
