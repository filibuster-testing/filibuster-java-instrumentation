package cloud.filibuster.unit;

import cloud.filibuster.examples.Hello;
import cloud.filibuster.exceptions.CircuitBreakerException;
import cloud.filibuster.junit.server.core.reports.ServerInvocationAndResponse;
import cloud.filibuster.junit.server.core.serializers.GeneratedMessageV3Serializer;
import cloud.filibuster.junit.server.core.serializers.StatusSerializer;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Status;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("UnnecessarilyFullyQualified")
public class SerializersTest {
    private static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    private static String generateFullMethodName() {
        return "cloud.filibuster.examples.Hello/PartialHello";
    }

    private static GeneratedMessageV3 generateRequestMessage() {
        return Hello.HelloRequest.newBuilder().setName("Chris").build();
    }

    private static Status generateResponseStatus() {
        return Status.OK;
    }

    private static Status generateResponseStatusWithFailureCodeAndDescription() {
        return Status.fromCode(Status.FAILED_PRECONDITION.getCode()).withDescription("blah");
    }

    private static Status generateResponseStatusWithFailureCause() {
        return Status.fromThrowable(new CircuitBreakerException("circuit breaker is opened"));
    }

    private static GeneratedMessageV3 generateResponseMessage() {
        return Hello.HelloReply.newBuilder().setMessage("Hi, Chris!").build();
    }

    private static ServerInvocationAndResponse generateServerInvocationAndResponse() {
        String requestId = generateRequestId();
        String fullMethodName = generateFullMethodName();
        GeneratedMessageV3 requestMessage = generateRequestMessage();
        Status responseStatus = generateResponseStatus();
        GeneratedMessageV3 responseMessage = generateResponseMessage();

        return new ServerInvocationAndResponse(
                requestId,
                fullMethodName,
                requestMessage,
                responseStatus,
                responseMessage
        );
    }

    private static ServerInvocationAndResponse generateServerInvocationAndResponseWithFailureCodeAndDescription() {
        String requestId = generateRequestId();
        String fullMethodName = generateFullMethodName();
        GeneratedMessageV3 requestMessage = generateRequestMessage();
        Status responseStatus = generateResponseStatusWithFailureCodeAndDescription();
        GeneratedMessageV3 responseMessage = generateResponseMessage();

        return new ServerInvocationAndResponse(
                requestId,
                fullMethodName,
                requestMessage,
                responseStatus,
                responseMessage
        );
    }

    private static ServerInvocationAndResponse generateServerInvocationAndResponseWithFailureCause() {
        String requestId = generateRequestId();
        String fullMethodName = generateFullMethodName();
        GeneratedMessageV3 requestMessage = generateRequestMessage();
        Status responseStatus = generateResponseStatusWithFailureCause();
        GeneratedMessageV3 responseMessage = generateResponseMessage();

        return new ServerInvocationAndResponse(
                requestId,
                fullMethodName,
                requestMessage,
                responseStatus,
                responseMessage
        );
    }

    @Test
    public void testCreation() {
        String requestId = generateRequestId();
        String fullMethodName = generateFullMethodName();
        GeneratedMessageV3 requestMessage = generateRequestMessage();
        Status responseStatus = generateResponseStatus();
        GeneratedMessageV3 responseMessage = generateResponseMessage();

        ServerInvocationAndResponse sir = new ServerInvocationAndResponse(
                requestId,
                fullMethodName,
                requestMessage,
                responseStatus,
                responseMessage
        );

        assertEquals(requestId, sir.getRequestId());
        assertEquals(fullMethodName, sir.getFullMethodName());
        assertEquals(requestMessage, sir.getRequestMessage());
        assertEquals(responseStatus, sir.getResponseStatus());
        assertEquals(responseMessage, sir.getResponseMessage());
    }

    @Test
    public void testMessagesToJsonWithOnlyGsonPayload() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponse();

        JSONObject expectedRequestMessageJSONObject = new JSONObject();
        expectedRequestMessageJSONObject.put("name", "Chris");

        JSONObject requestMessageJSONObject = GeneratedMessageV3Serializer.toJSONObjectWithOnlyPayload(sir.getRequestMessage());
        assertTrue(requestMessageJSONObject.similar(expectedRequestMessageJSONObject));

        JSONObject expectedResponseMessageJSONObject = new JSONObject();
        expectedResponseMessageJSONObject.put("message", "Hi, Chris!");

        JSONObject responseMessageJSONObject = GeneratedMessageV3Serializer.toJSONObjectWithOnlyPayload(sir.getResponseMessage());
        assertTrue(responseMessageJSONObject.similar(expectedResponseMessageJSONObject));
    }

    @Test
    public void testMessagesToJsonWithClassIncluded() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponse();

        JSONObject expectedRequestMessageJSONObjectGson = new JSONObject();
        expectedRequestMessageJSONObjectGson.put("name", "Chris");

        JSONObject expectedRequestMessageJSONObject = new JSONObject();
        expectedRequestMessageJSONObject.put("class", "cloud.filibuster.examples.Hello$HelloRequest");
        expectedRequestMessageJSONObject.put("gson", expectedRequestMessageJSONObjectGson);
        expectedRequestMessageJSONObject.put("toString", sir.getRequestMessage().toString());

        JSONObject requestMessageJSONObject = GeneratedMessageV3Serializer.toJSONObjectWithClassIncluded(sir.getRequestMessage());
        assertTrue(requestMessageJSONObject.similar(expectedRequestMessageJSONObject));
        assertTrue(GeneratedMessageV3Serializer.toJSONObject(sir.getRequestMessage()).similar(expectedRequestMessageJSONObject));

        JSONObject expectedResponseMessageJSONObjectGson = new JSONObject();
        expectedResponseMessageJSONObjectGson.put("message", "Hi, Chris!");

        JSONObject expectedResponseMessageJSONObject = new JSONObject();
        expectedResponseMessageJSONObject.put("class", "cloud.filibuster.examples.Hello$HelloReply");
        expectedResponseMessageJSONObject.put("gson", expectedResponseMessageJSONObjectGson);
        expectedResponseMessageJSONObject.put("toString", sir.getResponseMessage().toString());

        JSONObject responseMessageJSONObject = GeneratedMessageV3Serializer.toJSONObjectWithClassIncluded(sir.getResponseMessage());
        assertTrue(responseMessageJSONObject.similar(expectedResponseMessageJSONObject));
        assertTrue(GeneratedMessageV3Serializer.toJSONObject(sir.getResponseMessage()).similar(expectedResponseMessageJSONObject));
    }

    @Test
    public void testMessagesToJSONObject() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponse();

        JSONObject expectedRequestMessageJSONObjectGson = new JSONObject();
        expectedRequestMessageJSONObjectGson.put("name", "Chris");

        JSONObject expectedRequestMessageJSONObject = new JSONObject();
        expectedRequestMessageJSONObject.put("class", "cloud.filibuster.examples.Hello$HelloRequest");
        expectedRequestMessageJSONObject.put("gson", expectedRequestMessageJSONObjectGson);
        expectedRequestMessageJSONObject.put("toString", sir.getRequestMessage().toString());

        assertTrue(GeneratedMessageV3Serializer.toJSONObject(sir.getRequestMessage()).similar(expectedRequestMessageJSONObject));

        JSONObject expectedResponseMessageJSONObjectGson = new JSONObject();
        expectedResponseMessageJSONObjectGson.put("message", "Hi, Chris!");

        JSONObject expectedResponseMessageJSONObject = new JSONObject();
        expectedResponseMessageJSONObject.put("class", "cloud.filibuster.examples.Hello$HelloReply");
        expectedResponseMessageJSONObject.put("gson", expectedResponseMessageJSONObjectGson);
        expectedResponseMessageJSONObject.put("toString", sir.getResponseMessage().toString());

        assertTrue(GeneratedMessageV3Serializer.toJSONObject(sir.getResponseMessage()).similar(expectedResponseMessageJSONObject));
    }

    @Test
    public void testMessagesFromJSONObject() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponse();

        JSONObject expectedRequestMessageJSONObjectGson = new JSONObject();
        expectedRequestMessageJSONObjectGson.put("name", "Chris");

        JSONObject expectedRequestMessageJSONObject = new JSONObject();
        expectedRequestMessageJSONObject.put("class", "cloud.filibuster.examples.Hello$HelloRequest");
        expectedRequestMessageJSONObject.put("gson", expectedRequestMessageJSONObjectGson);
        expectedRequestMessageJSONObject.put("toString", sir.getRequestMessage().toString());

        JSONObject serializedRequestMessageToJSONObject = GeneratedMessageV3Serializer.toJSONObject(sir.getRequestMessage());
        assertTrue(serializedRequestMessageToJSONObject.similar(expectedRequestMessageJSONObject));

        GeneratedMessageV3 requestMessageFromJSONObject = GeneratedMessageV3Serializer.fromJSONObject(serializedRequestMessageToJSONObject);
        assertEquals(sir.getRequestMessage(), requestMessageFromJSONObject);

        JSONObject expectedResponseMessageJSONObjectGson = new JSONObject();
        expectedResponseMessageJSONObjectGson.put("message", "Hi, Chris!");

        JSONObject expectedResponseMessageJSONObject = new JSONObject();
        expectedResponseMessageJSONObject.put("class", "cloud.filibuster.examples.Hello$HelloReply");
        expectedResponseMessageJSONObject.put("gson", expectedResponseMessageJSONObjectGson);
        expectedResponseMessageJSONObject.put("toString", sir.getResponseMessage().toString());

        JSONObject serializedResponseMessageToJSONObject = GeneratedMessageV3Serializer.toJSONObject(sir.getResponseMessage());
        assertTrue(serializedResponseMessageToJSONObject.similar(expectedResponseMessageJSONObject));

        GeneratedMessageV3 responseMessageFromJSONObject = GeneratedMessageV3Serializer.fromJSONObject(serializedResponseMessageToJSONObject);
        assertEquals(sir.getResponseMessage(), responseMessageFromJSONObject);
    }

    @Test
    public void testStatusToJSONObject() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponse();
        JSONObject statusObject = StatusSerializer.toJsonObject(sir.getResponseStatus());

        JSONObject expectedStatusObject = new JSONObject();
        expectedStatusObject.put("class", "io.grpc.Status");
        expectedStatusObject.put("code", "OK");

        assertTrue(expectedStatusObject.similar(statusObject));
    }

    @Test
    public void testStatusFromJSONObject() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponse();
        JSONObject statusObject = StatusSerializer.toJsonObject(sir.getResponseStatus());

        JSONObject expectedStatusObject = new JSONObject();
        expectedStatusObject.put("class", "io.grpc.Status");
        expectedStatusObject.put("code", "OK");

        assertTrue(expectedStatusObject.similar(statusObject));

        io.grpc.Status status = StatusSerializer.fromJsonObject(statusObject);
        assertEquals(sir.getResponseStatus(), status);
    }

    @Test
    public void testStatusToJSONObjectWithFailureCodeAndDescription() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponseWithFailureCodeAndDescription();
        JSONObject statusObject = StatusSerializer.toJsonObject(sir.getResponseStatus());

        JSONObject expectedStatusObject = new JSONObject();
        expectedStatusObject.put("class", "io.grpc.Status");
        expectedStatusObject.put("code", "FAILED_PRECONDITION");
        expectedStatusObject.put("description", "blah");

        assertTrue(expectedStatusObject.similar(statusObject));
    }

    @Test
    public void testStatusFromJSONObjectWithFailureCodeAndDescription() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponseWithFailureCodeAndDescription();
        JSONObject statusObject = StatusSerializer.toJsonObject(sir.getResponseStatus());

        JSONObject expectedStatusObject = new JSONObject();
        expectedStatusObject.put("class", "io.grpc.Status");
        expectedStatusObject.put("code", "FAILED_PRECONDITION");
        expectedStatusObject.put("description", "blah");

        assertTrue(expectedStatusObject.similar(statusObject));

        io.grpc.Status status = StatusSerializer.fromJsonObject(statusObject);
        assertEquals(sir.getResponseStatus().getCode(), status.getCode());
        assertEquals(sir.getResponseStatus().getDescription(), status.getDescription());
    }

    @Test
    public void testStatusToJSONObjectWithFailureCause() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponseWithFailureCause();
        JSONObject statusObject = StatusSerializer.toJsonObject(sir.getResponseStatus());

        JSONObject expectedStatusObject = new JSONObject();
        expectedStatusObject.put("class", "io.grpc.Status");
        expectedStatusObject.put("code", "UNKNOWN");
        expectedStatusObject.put("cause", "cloud.filibuster.exceptions.CircuitBreakerException: circuit breaker is opened");

        assertTrue(expectedStatusObject.similar(statusObject));
    }

    @Test
    public void testStatusFromJSONObjectWithFailureCause() {
        ServerInvocationAndResponse sir = generateServerInvocationAndResponseWithFailureCause();
        JSONObject statusObject = StatusSerializer.toJsonObject(sir.getResponseStatus());

        JSONObject expectedStatusObject = new JSONObject();
        expectedStatusObject.put("class", "io.grpc.Status");
        expectedStatusObject.put("code", "UNKNOWN");
        expectedStatusObject.put("cause", "cloud.filibuster.exceptions.CircuitBreakerException: circuit breaker is opened");

        assertTrue(expectedStatusObject.similar(statusObject));

        io.grpc.Status status = StatusSerializer.fromJsonObject(statusObject);
        assertEquals(sir.getResponseStatus().getCode(), status.getCode());
        assertEquals(sir.getResponseStatus().getDescription(), status.getDescription());
        assertNull(status.getCause()); // cause does not serialize across service boundaries.
    }
}
