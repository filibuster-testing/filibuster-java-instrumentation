package cloud.filibuster.junit.server.core.invocations;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;

import javax.annotation.Nullable;

public class ServerInvocationAndResponse {
    private final String requestId;
    private final String fullMethodName;
    private final GeneratedMessageV3 requestMessage;
    private final Status responseStatus;
    @Nullable
    private GeneratedMessageV3 responseMessage;

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

}
