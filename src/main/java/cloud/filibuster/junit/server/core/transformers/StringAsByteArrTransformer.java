package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

public final class StringAsByteArrTransformer implements Transformer<byte[], Integer> {
    private boolean hasNext = true;
    private byte[] result;
    private Accumulator<byte[], Integer> accumulator;

    @Override
    @CanIgnoreReturnValue
    public StringAsByteArrTransformer transform(byte[] payload, Accumulator<byte[], Integer> accumulator) {
        int idx = accumulator.getContext();
        String payloadStr = new String(payload, Charset.defaultCharset());

        StringBuilder newString = new StringBuilder(payloadStr);
        newString.setCharAt(idx, getNextChar(newString.charAt(idx)));

        if (idx == payloadStr.length() - 1) {
            this.hasNext = false;
        }

        this.result = newString.toString().getBytes(Charset.defaultCharset());
        this.accumulator = accumulator;

        return this;
    }

    private static char getNextChar(char c) {
        // ASCII printable characters range from 33 to 126
        if (c == 126) {
            return 33;
        }
        return (char) (c + 1);
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
                Integer.class).getType();
    }

    @Override
    public Accumulator<byte[], Integer> getInitialAccumulator(byte[] referenceValue) {
        Accumulator<byte[], Integer> accumulator = new Accumulator<>();
        accumulator.setContext(0);
        accumulator.setReferenceValue(referenceValue);
        this.result = referenceValue;
        return accumulator;
    }

    @Override
    public Accumulator<byte[], Integer> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            accumulator.setContext(accumulator.getContext() + 1);
            return accumulator;
        }
    }
}
