package cloud.filibuster.junit.server.core.transformers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public final class ByteArrByzantineValueTransformer implements Transformer<byte[], Integer> {
    private boolean hasNext = true;
    private byte[] result;
    private Accumulator<byte[], Integer> accumulator;
    byte[][] possibleValues = {"".getBytes(StandardCharsets.UTF_8),
            "ThisIsATestString".getBytes(StandardCharsets.UTF_8),
            "abcd".getBytes(StandardCharsets.UTF_8),
            "1234!!".getBytes(StandardCharsets.UTF_8),
            "-11".getBytes(StandardCharsets.UTF_8),
            null};

    @Override
    @CanIgnoreReturnValue
    public ByteArrByzantineValueTransformer transform(byte[] payload, Accumulator<byte[], Integer> accumulator) {

        // Get the Byzantine value from the possible values arrays corresponding to the index in the context
        this.result = possibleValues[accumulator.getContext()];

        this.accumulator = accumulator;

        // Increment the context
        if (accumulator.getContext() < possibleValues.length - 1) {
            accumulator.setContext(accumulator.getContext() + 1);
        } else {
            this.hasNext = false;
        }

        return this;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Type getPayloadType() {
        return byte[].class;
    }

    @Override
    public byte[] getResult() {
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                byte[].class,
                Integer.class).getType();
    }

    @Override
    public Accumulator<byte[], Integer> getInitialAccumulator(byte[] referenceValue) {
        this.result = referenceValue;

        Accumulator<byte[], Integer> accumulator = new Accumulator<>();
        accumulator.setReferenceValue(referenceValue);
        accumulator.setContext(0); // Referring to the first entry in possibleValues array

        return accumulator;
    }

    @Override
    public Accumulator<byte[], Integer> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            return accumulator;
        }
    }
}
