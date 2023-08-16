package cloud.filibuster.junit.server.core.transformers;

import org.json.JSONObject;

import java.util.Iterator;

public final class JsonUtils {

    private static final String DELIMITER = "->";
    private static final String ESCAPE_DELIMITER = "->-";

    public static JSONObject flatten(JSONObject jsonObject) {
        JSONObject flattened = new JSONObject();
        flattenHelper(jsonObject, "", flattened);
        return flattened;
    }

    private static void flattenHelper(JSONObject obj, String prefix, JSONObject out) {
        Iterator<?> keys = obj.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String escapedKey = key.replace(DELIMITER, ESCAPE_DELIMITER);
            Object value = obj.get(key);
            if (value instanceof JSONObject) {
                flattenHelper((JSONObject) value, prefix + escapedKey + DELIMITER, out);
            } else {
                out.put(prefix + escapedKey, value);
            }
        }
    }

    public static JSONObject unflatten(JSONObject flattened) {
        JSONObject unflattened = new JSONObject();
        for (String key : flattened.keySet()) {
            String[] parts = key.split(DELIMITER, -1);
            JSONObject current = unflattened;
            for (int i = 0; i < parts.length - 1; i++) {
                String unescapedKey = parts[i].replace(ESCAPE_DELIMITER, DELIMITER);
                if (!current.has(unescapedKey)) {
                    current.put(unescapedKey, new JSONObject());
                }
                current = current.getJSONObject(unescapedKey);
            }
            String unescapedKey = parts[parts.length - 1].replace(ESCAPE_DELIMITER, DELIMITER);
            current.put(unescapedKey, flattened.get(key));
        }
        return unflattened;
    }
}
