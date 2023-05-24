package cloud.filibuster.junit.configuration.examples.redis.byzantine.types;

import com.google.common.primitives.Bytes;
import org.json.JSONArray;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ByzantineByteArrayFaultType extends ByzantineFaultType<byte[]> {
    @Override
    @Nullable
    public byte[] cast(Object byzantineFaultValue) {
        if (byzantineFaultValue == null) {
            return null;
        }
        List<Byte> byteArray = new ArrayList<>();
        // Cast the JSONArray to a byte array.
        ((JSONArray) byzantineFaultValue).toList().forEach(item -> byteArray.add(Byte.valueOf(item.toString())));
        return Bytes.toArray(byteArray);
    }

    @Override
    public ByzantineFault getFaultType() {
        return ByzantineFault.BYTE_ARRAY;
    }
}
