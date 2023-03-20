package cloud.filibuster.junit.server.core.serializers;

import cloud.filibuster.exceptions.filibuster.FilibusterDeserializationError;
import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessageV3;
import org.json.JSONObject;

public class GeneratedMessageV3Serializer {
    private static final Gson gson = new Gson();

    static class Keys {
        public static final String CLASS_KEY = "class";
        public static final String GSON_KEY = "gson";
        public static final String TO_STRING_KEY = "toString";
    }

    public static JSONObject toJSONObjectWithOnlyGsonPayload(GeneratedMessageV3 generatedMessageV3) {
        String gsonSerialized = gson.toJson(generatedMessageV3);
        return new JSONObject(gsonSerialized);
    }

    public static JSONObject toJSONObjectWithClassIncluded(GeneratedMessageV3 generatedMessageV3) {
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put(Keys.CLASS_KEY, generatedMessageV3.getClass().getName());
        newJSONObject.put(Keys.GSON_KEY, toJSONObjectWithOnlyGsonPayload(generatedMessageV3));
        newJSONObject.put(Keys.TO_STRING_KEY, generatedMessageV3.toString());
        return newJSONObject;
    }

    public static JSONObject toJSONObject(GeneratedMessageV3 generatedMessageV3) {
        return toJSONObjectWithClassIncluded(generatedMessageV3);
    }

    @SuppressWarnings("unchecked")
    public static GeneratedMessageV3 fromJSONObject(JSONObject jsonObject) {
        String className = jsonObject.getString(Keys.CLASS_KEY);
        JSONObject gsonPayload = jsonObject.getJSONObject(Keys.GSON_KEY);
        String gsonPayloadString = gsonPayload.toString();

        try {
            Class clazz = Class.forName(className);
            GeneratedMessageV3 target = (GeneratedMessageV3) gson.fromJson(gsonPayloadString, clazz);
            return target;
        } catch (ClassNotFoundException e) {
            throw new FilibusterDeserializationError("Failed to deserialize and instantiate information for the fake: " + e, e);
        }
    }
}
