package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import cloud.filibuster.exceptions.transformer.TransformerRuntimeException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public final class DBExceptionTransformer implements Transformer<Object, Integer> {
    private boolean hasNext = true;
    private Object result;
    private Accumulator<Object, Integer> accumulator;
    private static final ArrayList<DBException> dbExceptions = new ArrayList<>();

    @Override
    @CanIgnoreReturnValue
    public DBExceptionTransformer transform(Object payload, Accumulator<Object, Integer> accumulator) {

        // Get exception corresponding to idx in context
        this.result = dbExceptions.get(accumulator.getContext());
        this.accumulator = accumulator;

        // Increment context
        if (accumulator.getContext() < dbExceptions.size() - 1) {
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
        return Object.class;
    }

    @Override
    public Object getResult() {
        if (this.result == null) {
            throw new TransformerNullResultException("Result is null. getResult() was probably called before transform()!");
        }
        return this.result;
    }

    @Override
    public Type getAccumulatorType() {
        return TypeToken.getParameterized(
                Accumulator.class,
                Object.class,
                Integer.class).getType();
    }

    @Override
    public Accumulator<Object, Integer> getInitialAccumulator(Object referenceValue) {
        if (dbExceptions.size() == 0) {
            throw new TransformerRuntimeException("No DBExceptions were added to the DBExceptionTransformer!");
        }

        this.result = referenceValue;

        Accumulator<Object, Integer> accumulator = new Accumulator<>();
        accumulator.setReferenceValue(referenceValue);
        accumulator.setContext(0);

        return accumulator;
    }

    @Override
    public Accumulator<Object, Integer> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator(getResult());
        } else {
            return accumulator;
        }
    }

    public static void addDbException(DBException dbException) {
        DBExceptionTransformer.dbExceptions.add(dbException);
    }

}
