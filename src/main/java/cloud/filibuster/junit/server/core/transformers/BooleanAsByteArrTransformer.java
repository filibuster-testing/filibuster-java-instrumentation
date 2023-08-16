package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

public final class BooleanAsByteArrTransformer implements Transformer<byte[], String> {
    private boolean hasNext = true;
    private byte[] result;
    private Accumulator<byte[], String> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BooleanAsByteArrTransformer transform(byte[] payload, Accumulator<byte[], String> accumulator) {
        String payloadStr = new String(payload, Charset.defaultCharset());
        boolean boolValue = Boolean.parseBoolean(payloadStr);
        boolValue = !boolValue;

        this.result = String.valueOf(boolValue).getBytes(Charset.defaultCharset());
        this.accumulator = accumulator;

        this.hasNext = false;
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
        if (this.result == null) {
            throw new TransformerNullResultException("Result is null. getResult() was probably called before transform()!");
        }
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                byte[].class,
                String.class).getType();
    }

    @Override
    public Accumulator<byte[], String> getInitialAccumulator(byte[] referenceValue) {
        this.result = referenceValue;

        Accumulator<byte[], String> accumulator = new Accumulator<>();
        accumulator.setReferenceValue(referenceValue);

        return accumulator;
    }

    @Override
    public Accumulator<byte[], String> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            return accumulator;
        }
    }
}
