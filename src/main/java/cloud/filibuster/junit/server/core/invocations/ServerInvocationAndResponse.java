package cloud.filibuster.junit.server.core.invocations;

import cloud.filibuster.exceptions.filibuster.FilibusterFakeDeserializationError;
import com.google.gson.Gson;
import org.json.JSONObject;

import javax.annotation.Nullable;

@SuppressWarnings("UnnecessarilyFullyQualified")
public class ServerInvocationAndResponse {
    private final String requestId;
    private final String fullMethodName;
    private final com.google.protobuf.GeneratedMessageV3 requestMessage;
    private final io.grpc.Status responseStatus;
    @Nullable
    private final com.google.protobuf.GeneratedMessageV3 responseMessage;

    private static final Gson gson = new Gson();

    public ServerInvocationAndResponse(
            String requestId,
            String fullMethodName,
            com.google.protobuf.GeneratedMessageV3 requestMessage,
            io.grpc.Status responseStatus,
            com.google.protobuf.GeneratedMessageV3 responseMessage
    ) {
        this.requestId = requestId;
        this.fullMethodName = fullMethodName;
        this.requestMessage = requestMessage;
        this.responseStatus = responseStatus;
        this.responseMessage = responseMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getFullMethodName() {
        return fullMethodName;
    }

    public com.google.protobuf.GeneratedMessageV3 getRequestMessage() {
        return requestMessage;
    }

    public io.grpc.Status getResponseStatus() {
        return responseStatus;
    }

    @Nullable
    public com.google.protobuf.GeneratedMessageV3 getResponseMessage() {
        return responseMessage;
    }

    public static class GeneratedMessageV3 {
        public static JSONObject toJSONObjectWithOnlyGsonPayload(com.google.protobuf.GeneratedMessageV3 generatedMessageV3) {
            String gsonSerialized = gson.toJson(generatedMessageV3);
            return new JSONObject(gsonSerialized);
        }

        public static JSONObject toJSONObjectWithClassIncluded(com.google.protobuf.GeneratedMessageV3 generatedMessageV3) {
            JSONObject newJSONObject = new JSONObject();
            newJSONObject.put("class", generatedMessageV3.getClass().getName());
            newJSONObject.put("gson", toJSONObjectWithOnlyGsonPayload(generatedMessageV3));
            newJSONObject.put("toString", generatedMessageV3.toString());
            return newJSONObject;
        }

        public static JSONObject toJSONObject(com.google.protobuf.GeneratedMessageV3 generatedMessageV3) {
            return toJSONObjectWithClassIncluded(generatedMessageV3);
        }

        @SuppressWarnings("unchecked")
        public static com.google.protobuf.GeneratedMessageV3 fromJSONObject(JSONObject jsonObject) {
            String className = jsonObject.getString("class");
            JSONObject gsonPayload = jsonObject.getJSONObject("gson");
            String gsonPayloadString = gsonPayload.toString();

            try {
                Class clazz = Class.forName(className);
                com.google.protobuf.GeneratedMessageV3 target = (com.google.protobuf.GeneratedMessageV3) gson.fromJson(gsonPayloadString, clazz);
                return target;
            } catch (ClassNotFoundException e) {
                throw new FilibusterFakeDeserializationError("Failed to deserialize and instantiate information for the fake: " + e, e);
            }
        }
    }

    public static class Status {
        public static JSONObject toJSONObject(io.grpc.Status status) {
            JSONObject jsonObject = new JSONObject();

            // TODO: use keys.
            jsonObject.put("class", "io.grpc.Status");
            jsonObject.put("code", status.getCode().toString());
            jsonObject.put("cause", status.getCause());
            jsonObject.put("description", status.getDescription());

            return jsonObject;
        }

        public static io.grpc.Status fromJSONObject(JSONObject jsonObject) {
            String codeStr = jsonObject.getString("code");
            io.grpc.Status.Code code = io.grpc.Status.Code.valueOf(codeStr);
            return io.grpc.Status.fromCode(code);
        }
    }

    static class Keys {
        public static final String REQUEST_ID_KEY = "request_id";
        public static final String METHOD_KEY = "method";
        public static final String REQUEST_KEY = "request";
        public static final String STATUS_KEY = "status";
        public static final String RESPONSE_KEY = "response";
    }

    public JSONObject toJSONObject() {
        JSONObject result = new JSONObject();
        result.put(Keys.REQUEST_ID_KEY, requestId);
        result.put(Keys.METHOD_KEY, fullMethodName);

        if (requestMessage != null) {
            result.put(Keys.REQUEST_KEY, GeneratedMessageV3.toJSONObject(requestMessage));
        } else {
            result.put(Keys.REQUEST_KEY, new JSONObject());
        }

        if (responseStatus != null) {
            result.put(Keys.STATUS_KEY, Status.toJSONObject(responseStatus));
        } else {
            result.put(Keys.STATUS_KEY, new JSONObject());
        }

        if (responseMessage != null) {
            result.put(Keys.RESPONSE_KEY, GeneratedMessageV3.toJSONObject(responseMessage));
        } else {
            result.put(Keys.RESPONSE_KEY, new JSONObject());
        }

        return result;
    }
}