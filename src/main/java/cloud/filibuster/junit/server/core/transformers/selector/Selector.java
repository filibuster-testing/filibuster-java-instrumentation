package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.Transformer;
import com.google.gson.Gson;

import java.util.function.Function;
import java.util.regex.Pattern;

abstract class Selector {

    abstract <T> Class<? extends Transformer<?, ?>> select(T sReferenceValue);

    private static final Pattern boolPattern = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);

    private static final Gson gson = new Gson();

    @SuppressWarnings("ReturnValueIgnored")
    <CLASS, VALUE> boolean isApplicable(Class<CLASS> clazz, VALUE value, Function<CLASS, ?> func) {
        try {
            CLASS gsonValue = gson.fromJson(String.valueOf(value), clazz);
            func.apply(gsonValue);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    protected static boolean isBoolean(String value) {
        if (boolPattern.matcher(value).matches()) {
            return true;
        } else {
            throw new IllegalArgumentException("Value is not a boolean: " + value);
        }
    }
}
