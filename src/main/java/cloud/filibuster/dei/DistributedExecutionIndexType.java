package cloud.filibuster.dei;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;

import static cloud.filibuster.instrumentation.helpers.Property.getDeiVersionProperty;

public enum DistributedExecutionIndexType {
    V1 {
        @Override
        public DistributedExecutionIndex createImpl() {
            return new DistributedExecutionIndexV1();
        }
    };

    public abstract DistributedExecutionIndex createImpl();

    public static DistributedExecutionIndexType getImplType() {
        return getDeiVersionProperty();
    }
}
