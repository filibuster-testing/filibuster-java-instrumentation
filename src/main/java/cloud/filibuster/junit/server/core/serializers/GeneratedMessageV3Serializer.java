package cloud.filibuster.junit.server.core.serializers;

import cloud.filibuster.exceptions.filibuster.FilibusterDeserializationError;
import cloud.filibuster.exceptions.filibuster.FilibusterMessageSerializationException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneratedMessageV3Serializer {
    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(GeneratedMessageV3Serializer.class.getName());

    static class Keys {
        public static final String CLASS_KEY = "class";
        public static final String GSON_KEY = "gson";
        public static final String TO_STRING_KEY = "toString";
    }

    public static JSONObject toJSONObjectWithOnlyGsonPayload(GeneratedMessageV3 generatedMessageV3) {
        String serializedMessage;
        try {
            // Try to serialize the message using Gson
            serializedMessage = gson.toJson(generatedMessageV3);
        } catch (JsonIOException t) {
            // If the serialization fails, try to serialize the message using JsonFormat
            logger.log(Level.WARNING, "[toJSONObjectWithOnlyGsonPayload]: Failed to serialize message using gson, trying" +
                    "to serialize using JsonFormat instead: " + generatedMessageV3, t);
            try {
                serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(generatedMessageV3);
                logger.log(Level.INFO, "[toJSONObjectWithOnlyGsonPayload]: Successfully serialized message using JsonFormat: " + serializedMessage);
            } catch (InvalidProtocolBufferException e) {
                // If that fails as well, throw an exception and log it
                logger.log(Level.SEVERE, "[toJSONObjectWithOnlyGsonPayload]: Failed to serialize message using JsonFormat. Throwing... " + generatedMessageV3, e);
                throw new FilibusterMessageSerializationException("Failed to serialize message using JsonFormat: " + generatedMessageV3, e);
            }
        }
        return new JSONObject(serializedMessage);
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
