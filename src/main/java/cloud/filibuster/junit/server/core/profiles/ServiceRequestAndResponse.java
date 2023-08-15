package cloud.filibuster.junit.server.core.profiles;

import cloud.filibuster.junit.server.core.serializers.GeneratedMessageV3Serializer;
import cloud.filibuster.junit.server.core.serializers.StatusSerializer;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Objects;

public class ServiceRequestAndResponse {
    private final GeneratedMessageV3 request;
    private final Status status;
    @Nullable
    private final GeneratedMessageV3 response;

    public ServiceRequestAndResponse(
            GeneratedMessageV3 request,
            Status status,
            GeneratedMessageV3 response
    ) {
        this.request = request;
        this.status = status;
        this.response = response;
    }

    public boolean isSuccess() {
        if (this.status.getCode().equals(Status.OK.getCode())) {
            return true;
        }

        return false;
    }

    public Status getStatus() {
        return this.status;
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("request", GeneratedMessageV3Serializer.toJsonObject(request));
        jsonObject.put("status", StatusSerializer.toJsonObject(status));

        if (response != null) {
            jsonObject.put("response", GeneratedMessageV3Serializer.toJsonObject(response));
        }

        return jsonObject;
    }

    public static ServiceRequestAndResponse fromJsonObject(JSONObject jsonObject) {
        JSONObject requestObject = jsonObject.getJSONObject("request");
        GeneratedMessageV3 request = GeneratedMessageV3Serializer.fromJsonObject(requestObject);

        JSONObject statusObject = jsonObject.getJSONObject("status");
        Status status = StatusSerializer.fromJsonObject(statusObject);

        GeneratedMessageV3 response = null;

        if (jsonObject.has("response")) {
            JSONObject responseObject = jsonObject.getJSONObject("response");
            response = GeneratedMessageV3Serializer.fromJsonObject(responseObject);
        }

        return new ServiceRequestAndResponse(request, status, response);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof ServiceRequestAndResponse)) {
            return false;
        }

        ServiceRequestAndResponse srr = (ServiceRequestAndResponse) o;

        return Objects.equals(this.request, srr.request) && Objects.equals(this.status, srr.status) && Objects.equals(this.response, srr.response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.request, this.status, this.response);
    }

}
