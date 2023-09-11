package cloud.filibuster.unit.daikon;

import cloud.filibuster.daikon.DaikonGrpcDataTraceRecord;
import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.examples.Hello;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DaikonTest {
    @Test
    public void testDaikon() {
        // Setup.
        MethodDescriptor methodDescriptor = APIServiceGrpc.getCreateSessionMethod();
        String nonceString = "CHANGE-ME-NONCE";

        // Entry example.
        Hello.CreateSessionRequest createSessionRequest = Hello.CreateSessionRequest.newBuilder().setUserId("1").build();
        DaikonGrpcDataTraceRecord daikonGrpcDataTraceRecordForRequest = DaikonGrpcDataTraceRecord.onRequest(nonceString, methodDescriptor.getFullMethodName(), createSessionRequest);
        String serializedTraceRecordForRequest = daikonGrpcDataTraceRecordForRequest.toString();
        assertFalse(serializedTraceRecordForRequest.isEmpty());

        assertEquals("cloud.filibuster.examples.APIService.CreateSession(CreateSessionRequest):::ENTER\n" +
                "this_invocation_nonce\n" +
                "CHANGE-ME-NONCE\n" +
                "location\n" +
                "\"\"\n" +
                "1\n" +
                "userId\n" +
                "\"1\"\n" +
                "1\n", serializedTraceRecordForRequest);

        // Exit example.
        Hello.CreateSessionResponse createSessionResponse = Hello.CreateSessionResponse.newBuilder().setSessionId("the-session-id").build();
        DaikonGrpcDataTraceRecord daikonGrpcDataTraceRecordForResponse = DaikonGrpcDataTraceRecord.onResponse(nonceString, methodDescriptor.getFullMethodName(), createSessionResponse);
        String serializedTraceRecordForResponse = daikonGrpcDataTraceRecordForResponse.toString();
        assertFalse(serializedTraceRecordForResponse.isEmpty());

        assertEquals("cloud.filibuster.examples.APIService.CreateSession(CreateSessionResponse):::EXIT0\n" +
                "this_invocation_nonce\n" +
                "CHANGE-ME-NONCE\n" +
                "sessionId\n" +
                "\"the-session-id\"\n" +
                "1\n" +
                "sessionSize\n" +
                "\"0\"\n" +
                "1\n", serializedTraceRecordForResponse);
    }
}
