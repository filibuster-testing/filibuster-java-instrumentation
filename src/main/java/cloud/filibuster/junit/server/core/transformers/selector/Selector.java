package cloud.filibuster.junit.server.core.transformers.selector;

import cloud.filibuster.junit.server.core.transformers.Transformer;
import com.google.gson.Gson;

import java.util.function.Function;

abstract class Selector {

    abstract Class<? extends Transformer<?, ?>> select(String sReferenceValue);
    private static final Gson gson = new Gson();

    @SuppressWarnings("ReturnValueIgnored")
    <T> boolean isApplicable(Class<T> clazz, String value, Function<T, ?> func) {
        try {
            T gsonValue = gson.fromJson(value, clazz);
            func.apply(gsonValue);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
