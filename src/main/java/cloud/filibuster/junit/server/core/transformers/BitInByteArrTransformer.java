package cloud.filibuster.junit.server.core.transformers;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONObject;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Random;

public final class BitInByteArrTransformer implements Transformer<Byte[], JSONObject> {
    private static final long FIXED_SEED = 0;
    private static final Random rand = new Random(FIXED_SEED); // Seed is fixed to ensure consistent results
    private boolean hasNext = true;
    private Byte[] result;
    private static final String LAST_ADDED_KEY = "LAST_ADDED_KEY";
    private static final String KEYS_HASHMAP = "KEYS_HASHMAP";
    private Accumulator<Byte[], JSONObject> accumulator;

    @Override
    @CanIgnoreReturnValue
    public BitInByteArrTransformer transform(Byte[] payload, Accumulator<Byte[], JSONObject> accumulator) {
        JSONObject ctx = accumulator.getContext();

        @SuppressWarnings("unchecked")
        SimpleEntry<Integer, Integer> byteBit = (SimpleEntry<Integer, Integer>) ctx.get(LAST_ADDED_KEY);

        byte myByte = payload[byteBit.getKey()];

        String myBits = String.format("%8s", Integer.toBinaryString(myByte & 0xFF)).replace(' ', '0');

        StringBuilder myMutatedBits = new StringBuilder(myBits);
        if (myMutatedBits.charAt(byteBit.getValue()) == '0') {
            myMutatedBits.setCharAt(byteBit.getValue(), '1');
        } else {
            myMutatedBits.setCharAt(byteBit.getValue(), '0');
        }

        payload[byteBit.getKey()] = Byte.parseByte(myMutatedBits.toString(), 2);

        result = payload;

        if (ctx.keySet().size() == payload.length * 8) {
            this.hasNext = false;
        }

        return this;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Class<Byte[]> getPayloadType() {
        return Byte[].class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<JSONObject> getContextType() {
        return JSONObject.class;
    }

    @Override
    public Byte[] getResult() {
        if (this.result == null) {
            throw new FilibusterFaultInjectionException("getResult() called before transform()!");
        }
        return this.result;
    }

    @Override
    public Accumulator<Byte[], JSONObject> getAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        }
        return this.accumulator;
    }

    @Override
    public Accumulator<Byte[], JSONObject> getInitialAccumulator() {
        Accumulator<Byte[], JSONObject> accumulator = new Accumulator<>();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(LAST_ADDED_KEY, new SimpleEntry<>(null, null));
        jsonObject.put(KEYS_HASHMAP, new HashMap<>());
        accumulator.setContext(jsonObject);
        return accumulator;
    }

    @Override
    public Accumulator<Byte[], JSONObject> getNextAccumulator() {
        if (this.accumulator == null) {
            return getInitialAccumulator();
        } else {
            JSONObject ctx = accumulator.getContext();

            @SuppressWarnings("unchecked")
            HashMap<Integer, Integer> previousEntries = (HashMap<Integer, Integer>) ctx.get(KEYS_HASHMAP);

            @SuppressWarnings("unchecked")
            SimpleEntry<Integer, Integer> lastEntry = (SimpleEntry<Integer, Integer>) ctx.get(LAST_ADDED_KEY);

            int byteIdx = generateRandomIdx(accumulator.getReferenceValue().length);
            int bitIdx = new Random().nextInt(8);

            while (previousEntries.containsKey(byteIdx) && !previousEntries.get(byteIdx).equals(bitIdx)) {
                byteIdx = new Random().nextInt(accumulator.getReferenceValue().length);
                bitIdx = new Random().nextInt(8);
                if (previousEntries.containsKey(byteIdx) && previousEntries.get(byteIdx).equals(bitIdx)) {
                    break;
                }
            }

            previousEntries.put(byteIdx, bitIdx);
            ctx.put(LAST_ADDED_KEY, new SimpleEntry<>(byteIdx, bitIdx));
            ctx.put(KEYS_HASHMAP, previousEntries);

            accumulator.setContext(ctx);
            return accumulator;
        }
    }

    private static int generateRandomIdx(int bound) {
        return new Random().nextInt(bound);
    }

}
