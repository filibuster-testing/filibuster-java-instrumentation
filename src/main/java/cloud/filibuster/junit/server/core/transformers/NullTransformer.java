package cloud.filibuster.junit.server.core.transformers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

public final class NullTransformer implements Transformer<Object, Object> {
    @Nullable
    private Object result;

    @CanIgnoreReturnValue
    @Override
    public NullTransformer transform(Object payload, Accumulator<Object, Object> accumulator) {

        this.result = null;  // Result of transformation is always null

        return this;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Type getPayloadType() {
        return Object.class;
    }

    @Override
    @Nullable
    public Object getResult() {
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                Object.class,
                Object.class).getType();
    }

    @Override
    public Accumulator<Object, Object> getInitialAccumulator(Object referenceValue) {
        this.result = referenceValue;

        Accumulator<Object, Object> accumulator = new Accumulator<>();
        accumulator.setReferenceValue(referenceValue);

        return accumulator;
    }

    @Override
    public Accumulator<Object, Object> getNextAccumulator() {
        return getInitialAccumulator(getResult());
    }
}
