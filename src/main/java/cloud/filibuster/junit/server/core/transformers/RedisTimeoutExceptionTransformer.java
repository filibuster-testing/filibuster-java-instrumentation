package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.transformer.TransformerNullResultException;
import cloud.filibuster.exceptions.transformer.TransformerRuntimeException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class RedisTimeoutExceptionTransformer implements Transformer<Object, Integer> {
    private boolean hasNext = true;
    private Object result;
    private Accumulator<Object, Integer> accumulator;
    private static final ArrayList<DBException> dbExceptions = new ArrayList<>();

    static {
        DBException.Builder commandTimeoutBuilder = new DBException.Builder();
        commandTimeoutBuilder.name("io.lettuce.core.RedisCommandTimeoutException");

        Map<String, String> exceptionMap = new HashMap<>();
        exceptionMap.put("cause", "Command timed out after 100 millisecond(s)");
        exceptionMap.put("code", "");
        commandTimeoutBuilder.metadata(exceptionMap);

        DBException commandTimeoutException = commandTimeoutBuilder.build();
        dbExceptions.add(commandTimeoutException);
    }

    @Override
    @CanIgnoreReturnValue
    public RedisTimeoutExceptionTransformer transform(Object payload, Accumulator<Object, Integer> accumulator) {

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

}
