package cloud.filibuster.junit.server.core.transformers;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public final class NullTransformer implements Transformer<Object, Object> {
    private Object result;

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
    public Accumulator<Object, Object> getInitialAccumulator() {
        return new Accumulator<>();
    }

    @Override
    public Accumulator<Object, Object> getNextAccumulator() {
        return getInitialAccumulator();
    }
}
