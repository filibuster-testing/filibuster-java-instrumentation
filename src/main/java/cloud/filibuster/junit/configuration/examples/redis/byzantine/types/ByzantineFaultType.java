package cloud.filibuster.junit.configuration.examples.redis.byzantine.types;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

public abstract class ByzantineFaultType<T> {

    // Cast byzantineFaultValue to the target type T
    abstract public T cast(Object byzantineFaultValue);

    // Return the fault type as ByzantineFault enum
    abstract ByzantineFault getFaultType();

    // Get ByzantineFaultType object from a string representing the fault type encoded in the enum ByzantineFault
    public static ByzantineFaultType<?> fromFaultType(String stringFaultType) {
        ByzantineFault faultType = ByzantineFault.valueOf(stringFaultType);
        switch (faultType) {
            case STRING:
                return new ByzantineStringFaultType();
            case BYTE_ARRAY:
                return new ByzantineByteArrayFaultType();
        }
        throw new FilibusterRuntimeException("Unknown ByzantineFaultType: " + faultType);
    }

    @Override
    public String toString() {
        return getFaultType().toString();
    }
}
