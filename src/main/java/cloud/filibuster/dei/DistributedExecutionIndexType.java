package cloud.filibuster.dei;

import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV2;

import static cloud.filibuster.instrumentation.helpers.Property.getDeiVersionProperty;

public enum DistributedExecutionIndexType {
    V1 {
        @Override
        public DistributedExecutionIndex createImpl() {
            return new DistributedExecutionIndexV1();
        }
    },

    V2 {
        @Override
        public DistributedExecutionIndex createImpl() {
            return new DistributedExecutionIndexV2();
        }
    };

    public abstract DistributedExecutionIndex createImpl();

    public static DistributedExecutionIndexType getImplType() {
        return getDeiVersionProperty();
    }
}
