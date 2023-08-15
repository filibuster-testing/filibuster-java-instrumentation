package cloud.filibuster.junit.server.core.reports;

import cloud.filibuster.junit.server.core.serializers.GeneratedMessageV3Serializer;
import cloud.filibuster.junit.server.core.serializers.StatusSerializer;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class ServerInvocationAndResponse {
    private final String requestId;
    private final String fullMethodName;
    private final GeneratedMessageV3 requestMessage;
    private final Status responseStatus;
    @Nullable
    private final GeneratedMessageV3 responseMessage;

    public ServerInvocationAndResponse(
            String requestId,
            String fullMethodName,
            GeneratedMessageV3 requestMessage,
            Status responseStatus,
            GeneratedMessageV3 responseMessage
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

    public GeneratedMessageV3 getRequestMessage() {
        return requestMessage;
    }

    public Status getResponseStatus() {
        return responseStatus;
    }

    @Nullable
    public GeneratedMessageV3 getResponseMessage() {
        return responseMessage;
    }

    static class Keys {
        public static final String REQUEST_ID_KEY = "request_id";
        public static final String METHOD_KEY = "method";
        public static final String REQUEST_KEY = "request";
        public static final String STATUS_KEY = "status";
        public static final String RESPONSE_KEY = "response";
    }

    public JSONObject toJsonObject() {
        JSONObject result = new JSONObject();
        result.put(Keys.REQUEST_ID_KEY, requestId);
        result.put(Keys.METHOD_KEY, fullMethodName);

        if (requestMessage != null) {
            result.put(Keys.REQUEST_KEY, GeneratedMessageV3Serializer.toJsonObject(requestMessage));
        } else {
            result.put(Keys.REQUEST_KEY, new JSONObject());
        }

        if (responseStatus != null) {
            result.put(Keys.STATUS_KEY, StatusSerializer.toJsonObject(responseStatus));
        } else {
            result.put(Keys.STATUS_KEY, new JSONObject());
        }

        if (responseMessage != null) {
            result.put(Keys.RESPONSE_KEY, GeneratedMessageV3Serializer.toJsonObject(responseMessage));
        } else {
            result.put(Keys.RESPONSE_KEY, new JSONObject());
        }

        return result;
    }
}