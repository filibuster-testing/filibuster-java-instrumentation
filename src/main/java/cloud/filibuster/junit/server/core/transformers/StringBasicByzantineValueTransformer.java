package cloud.filibuster.junit.server.core.transformers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public final class StringBasicByzantineValueTransformer implements Transformer<String, Integer> {
    private boolean hasNext = true;
    private String result;
    private Accumulator<String, Integer> accumulator;
    private static final String[] possibleValues = {null, ""};

    @Override
    @CanIgnoreReturnValue
    public StringBasicByzantineValueTransformer transform(String payload, Accumulator<String, Integer> accumulator) {

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
        return String.class;
    }

    @Override
    public String getResult() {
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                String.class,
                Integer.class).getType();
    }

    @Override
    public Accumulator<String, Integer> getInitialAccumulator(String referenceValue) {
        this.result = referenceValue;

        Accumulator<String, Integer> accumulator = new Accumulator<>();
        accumulator.setReferenceValue(referenceValue);
        accumulator.setContext(0); // Referring to the first entry in possibleValues array

        return accumulator;
    }

    @Override
    public Accumulator<String, Integer> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            return accumulator;
        }
    }
}
