package cloud.filibuster.dei;

import cloud.filibuster.instrumentation.datatypes.Callsite;

public interface DistributedExecutionIndex extends Cloneable {

    DistributedExecutionIndexKey convertCallsiteToDistributedExecutionIndexKey(Callsite callsite);

    void push(Callsite callsite);

    void pop();

    DistributedExecutionIndex deserialize(String serialized);

    Object clone();

}
