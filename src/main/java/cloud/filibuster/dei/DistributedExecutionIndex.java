package cloud.filibuster.dei;

import cloud.filibuster.instrumentation.datatypes.Callsite;

public interface DistributedExecutionIndex extends Cloneable {

    String serializeCallsite(Callsite callsite);

    // TODO: change interface
    void push(String entry);

    void push(Callsite callsite);

    void pop();

    DistributedExecutionIndex deserialize(String serialized);

    Object clone();

}
