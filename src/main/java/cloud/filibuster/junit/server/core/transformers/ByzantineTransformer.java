package cloud.filibuster.junit.server.core.transformers;

import org.json.JSONObject;

import javax.annotation.Nonnull;

public interface ByzantineTransformer<PAYLOAD>  {
    PAYLOAD transform(PAYLOAD payload, @Nonnull JSONObject accumulator);
    JSONObject getNewAccumulator();
    boolean hasNext();
    Class<PAYLOAD> getType();
}