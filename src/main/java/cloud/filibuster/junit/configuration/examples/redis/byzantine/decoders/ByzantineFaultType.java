package cloud.filibuster.junit.configuration.examples.redis.byzantine.decoders;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;

public abstract class ByzantineFaultType<T> {
    abstract public T cast(Object byzantineFaultValue);
    abstract ByzantineFault getFaultType();
    public static ByzantineFaultType<?> fromFaultType(String stringFaultType) {
        ByzantineFault faultType = ByzantineFault.valueOf(stringFaultType);
        switch (faultType) {
            case STRING:
                return new ByzantineStringFaultType();
            case BYTE_ARRAY:
                return new ByzantineByteArrayFaultType();
        }
        throw new FilibusterRuntimeException("Unknown ByzantineDecoder: " + faultType);
    }
    @Override
    public String toString() {
        return getFaultType().toString();
    }
}
