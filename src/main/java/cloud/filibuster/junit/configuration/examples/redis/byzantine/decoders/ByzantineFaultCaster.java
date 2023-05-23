package cloud.filibuster.junit.configuration.examples.redis.byzantine.decoders;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

public abstract class ByzantineFaultCaster<T> {
    abstract public T cast(Object byzantineFaultValue);
    abstract ByzantineFaultType getFaultType();
    public static ByzantineFaultCaster<?> fromFaultType(String stringFaultType) {
        ByzantineFaultType faultType = ByzantineFaultType.valueOf(stringFaultType);
        switch (faultType) {
            case STRING:
                return new ByzantineStringFault();
            case BYTE_ARRAY:
                return new ByzantineByteArrayFault();
        }
        throw new FilibusterRuntimeException("Unknown ByzantineDecoder: " + faultType);
    }
    @Override
    public String toString() {
        return getFaultType().toString();
    }
}
