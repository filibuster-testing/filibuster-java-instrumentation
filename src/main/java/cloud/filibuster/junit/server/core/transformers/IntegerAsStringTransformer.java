package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Random;

import static cloud.filibuster.instrumentation.helpers.Property.getRandomSeedProperty;

public final class IntegerAsStringTransformer implements Transformer<Integer, String> {
    private boolean hasNext = true;
    private Integer result;
    private Accumulator<Integer, String> accumulator;
    private static final Random rand = new Random(getRandomSeedProperty());

    @Override
    @CanIgnoreReturnValue
    public IntegerAsStringTransformer transform(Integer payload, Accumulator<Integer, String> accumulator) {

        this.result = payload * rand.nextInt();
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
        return Integer.class;
    }

    @Override
    public Integer getResult() {
        if (this.result == null) {
            throw new TransformerNullResultException("Result is null. getResult() was probably called before transform()!");
        }
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                Integer.class,
                String.class).getType();
    }

    @Override
    public Accumulator<Integer, String> getInitialAccumulator(Integer referenceValue) {
        this.result = referenceValue;

        Accumulator<Integer, String> accumulator = new Accumulator<>();
        accumulator.setReferenceValue(referenceValue);

        return accumulator;
    }

    @Override
    public Accumulator<Integer, String> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            return accumulator;
        }
    }
}
