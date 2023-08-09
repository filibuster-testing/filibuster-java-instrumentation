package cloud.filibuster.junit.server.core.serializers;

import cloud.filibuster.exceptions.filibuster.FilibusterDeserializationError;
import cloud.filibuster.exceptions.filibuster.FilibusterMessageSerializationException;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneratedMessageV3Serializer {
    private static final Logger logger = Logger.getLogger(GeneratedMessageV3Serializer.class.getName());

    static class Keys {
        public static final String CLASS_KEY = "class";
        public static final String GSON_KEY = "gson";
        public static final String TO_STRING_KEY = "toString";
    }

    public static JSONObject toJSONObjectWithOnlyPayload(GeneratedMessageV3 generatedMessageV3) {
        try {
            String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(generatedMessageV3);
            return new JSONObject(serializedMessage);
        } catch (InvalidProtocolBufferException e) {
            logger.log(Level.SEVERE, "[toJSONObjectWithOnlyGsonPayload]: Failed to serialize message using JsonFormat. Throwing... " + generatedMessageV3, e);
            throw new FilibusterMessageSerializationException("Failed to serialize message using JsonFormat: " + generatedMessageV3, e);
        }
    }

    public static JSONObject toJSONObjectWithClassIncluded(GeneratedMessageV3 generatedMessageV3) {
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put(Keys.CLASS_KEY, generatedMessageV3.getClass().getName());
        newJSONObject.put(Keys.GSON_KEY, toJSONObjectWithOnlyPayload(generatedMessageV3));
        newJSONObject.put(Keys.TO_STRING_KEY, generatedMessageV3.toString());
        return newJSONObject;
    }

    public static JSONObject toJSONObject(GeneratedMessageV3 generatedMessageV3) {
        return toJSONObjectWithClassIncluded(generatedMessageV3);
    }

    public static GeneratedMessageV3 fromJSONObject(JSONObject jsonObject) {
        String className = jsonObject.getString(Keys.CLASS_KEY);
        JSONObject gsonPayload = jsonObject.getJSONObject(Keys.GSON_KEY);
        String gsonPayloadString = gsonPayload.toString();

        try {
            Class<?> clazz = Class.forName(className);
            AbstractMessage.Builder<?> messageBuilder = (AbstractMessage.Builder<?>) clazz.getMethod("newBuilder").invoke(null);
            JsonFormat.parser().merge(gsonPayloadString, messageBuilder);
            return (GeneratedMessageV3) messageBuilder.build();
        } catch (ClassNotFoundException | InvalidProtocolBufferException | IllegalAccessException |
                 IllegalArgumentException |
                 InvocationTargetException
                 | NoSuchMethodException | SecurityException e) {
            throw new FilibusterDeserializationError("Failed to deserialize and instantiate information for the fake: " + e, e);
        }
    }
}
