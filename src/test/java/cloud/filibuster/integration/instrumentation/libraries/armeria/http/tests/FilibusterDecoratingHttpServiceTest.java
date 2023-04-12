package cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests;

import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;

import cloud.filibuster.instrumentation.datatypes.RequestId;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;

import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.RequestHeadersBuilder;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;

import org.json.JSONObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;

import java.io.IOException;
import java.util.Objects;

import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_VCLOCK;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_REQUEST_ID;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_ORIGIN_VCLOCK;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_EXECUTION_INDEX;

import static cloud.filibuster.integration.instrumentation.TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startMockFilibusterServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilibusterDecoratingHttpServiceTest extends FilibusterDecoratingHttpTest {
    @BeforeAll
    public static void disableThreadLocals() {
        ThreadLocalContextStorage.useGlobalContext = true;
    }

    @BeforeAll
    public static void startServices() throws IOException, InterruptedException {
        startHelloServerAndWaitUntilAvailable();
        startMockFilibusterServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopServices() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
        stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    @AfterAll
    public static void enableThreadLocals() {
        ThreadLocalContextStorage.useGlobalContext = false;
    }

    @AfterAll
    public static void resetAllFilibusterSettings() {
        ThreadLocalContextStorage.useGlobalContext = false;
        FilibusterDecoratingHttpService.disableServerCommunication = false;
        FilibusterDecoratingHttpService.disableInstrumentation = false;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
    }

    @BeforeEach
    public void resetFilibusterSettings() {
        FilibusterDecoratingHttpService.disableServerCommunication = false;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
        FilibusterDecoratingHttpService.disableInstrumentation = false;
    }

    @Test
    @DisplayName("Test hello server index route without remote call and missing clocks.")
    public void testIndexWithMissingClocks() {
        FilibusterDecoratingHttpService.disableServerCommunication = true;

        String requestId = RequestId.generateNewRequestId().toString();
        int generatedId = 0;
        VectorClock vclock = new VectorClock();
        VectorClock originVclock = new VectorClock();
        String distributedExecutionIndexStr = createNewDistributedExecutionIndex().toString();
        String sleepIntervalStr = "1";

        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        RequestHeadersBuilder additionalHeaders = getHeaders.toBuilder();
        additionalHeaders.add("X-Filibuster-Request-Id", requestId);
        additionalHeaders.add("X-Filibuster-Generated-Id", String.valueOf(generatedId));
        additionalHeaders.add("X-Filibuster-Execution-Index", distributedExecutionIndexStr);
        additionalHeaders.add("X-Filibuster-Forced-Sleep", sleepIntervalStr);
        AggregatedHttpResponse response = webClient.execute(additionalHeaders.build()).aggregate().join();

        // Get headers and verify a 200 response which means that delegation to the underlying callsite works.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assert pre-populated values are set into the thread context.
        assertEquals(requestId, ThreadLocalContextStorage.get(FILIBUSTER_REQUEST_ID));
        assertEquals(distributedExecutionIndexStr, ThreadLocalContextStorage.get(FILIBUSTER_EXECUTION_INDEX));
        assertEquals(vclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_VCLOCK)).toString());
        assertEquals(originVclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_ORIGIN_VCLOCK)).toString());
    }

    @Test
    @DisplayName("Test hello server index route without remote call.")
    public void testIndexWithoutRemoteCommunication() {
        FilibusterDecoratingHttpService.disableServerCommunication = true;

        String requestId = RequestId.generateNewRequestId().toString();
        int generatedId = 0;
        VectorClock vclock = new VectorClock();
        VectorClock originVclock = new VectorClock();
        String distributedExecutionIndexStr = createNewDistributedExecutionIndex().toString();
        String sleepIntervalStr = "1";

        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        RequestHeadersBuilder additionalHeaders = getHeaders.toBuilder();
        additionalHeaders.add("X-Filibuster-Request-Id", requestId);
        additionalHeaders.add("X-Filibuster-Generated-Id", String.valueOf(generatedId));
        additionalHeaders.add("X-Filibuster-VClock", vclock.toString());
        additionalHeaders.add("X-Filibuster-Origin-VClock", originVclock.toString());
        additionalHeaders.add("X-Filibuster-Execution-Index", distributedExecutionIndexStr);
        additionalHeaders.add("X-Filibuster-Forced-Sleep", sleepIntervalStr);
        AggregatedHttpResponse response = webClient.execute(additionalHeaders.build()).aggregate().join();

        // Get headers and verify a 200 response which means that delegation to the underlying callsite works.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assert pre-populated values are set into the thread context.
        assertEquals(requestId, ThreadLocalContextStorage.get(FILIBUSTER_REQUEST_ID));
        assertEquals(distributedExecutionIndexStr, ThreadLocalContextStorage.get(FILIBUSTER_EXECUTION_INDEX));
        assertEquals(vclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_VCLOCK)).toString());
        assertEquals(originVclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_ORIGIN_VCLOCK)).toString());
    }

    @Test
    @DisplayName("Test hello server index route.")
    public void testIndex() {
        String requestId = RequestId.generateNewRequestId().toString();
        int generatedId = 0;
        VectorClock vclock = new VectorClock();
        VectorClock originVclock = new VectorClock();
        String distributedExecutionIndexStr = createNewDistributedExecutionIndex().toString();
        String sleepIntervalStr = "1";

        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        RequestHeadersBuilder additionalHeaders = getHeaders.toBuilder();
        additionalHeaders.add("X-Filibuster-Request-Id", requestId);
        additionalHeaders.add("X-Filibuster-Generated-Id", String.valueOf(generatedId));
        additionalHeaders.add("X-Filibuster-VClock", vclock.toString());
        additionalHeaders.add("X-Filibuster-Origin-VClock", originVclock.toString());
        additionalHeaders.add("X-Filibuster-Execution-Index", distributedExecutionIndexStr);
        additionalHeaders.add("X-Filibuster-Forced-Sleep", sleepIntervalStr);
        AggregatedHttpResponse response = webClient.execute(additionalHeaders.build()).aggregate().join();

        // Get headers and verify a 200 response which means that delegation to the underlying callsite works.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assert pre-populated values are set into the thread context.
        assertEquals(requestId, ThreadLocalContextStorage.get(FILIBUSTER_REQUEST_ID));
        assertEquals(distributedExecutionIndexStr, ThreadLocalContextStorage.get(FILIBUSTER_EXECUTION_INDEX));
        assertEquals(vclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_VCLOCK)).toString());
        assertEquals(originVclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_ORIGIN_VCLOCK)).toString());

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals(generatedId, lastPayload.getInt("generated_id"));
        assertEquals("request_received", lastPayload.getString("instrumentation_type"));
        assertEquals("hello", lastPayload.getString("target_service_name"));
        assertEquals(distributedExecutionIndexStr, lastPayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test hello server index route with failed remote call.")
    public void testIndexWithFailedRemoteCommunication() {
        String requestId = RequestId.generateNewRequestId().toString();
        int generatedId = 0;
        VectorClock vclock = new VectorClock();
        VectorClock originVclock = new VectorClock();
        String distributedExecutionIndexStr = createNewDistributedExecutionIndex().toString();
        String sleepIntervalStr = "1";

        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        RequestHeadersBuilder additionalHeaders = getHeaders.toBuilder();
        additionalHeaders.add("X-Filibuster-Request-Id", requestId);
        additionalHeaders.add("X-Filibuster-Generated-Id", String.valueOf(generatedId));
        additionalHeaders.add("X-Filibuster-VClock", vclock.toString());
        additionalHeaders.add("X-Filibuster-Origin-VClock", originVclock.toString());
        additionalHeaders.add("X-Filibuster-Execution-Index", distributedExecutionIndexStr);
        additionalHeaders.add("X-Filibuster-Forced-Sleep", sleepIntervalStr);
        AggregatedHttpResponse response = webClient.execute(additionalHeaders.build()).aggregate().join();

        // Get headers and verify a 200 response which means that delegation to the underlying callsite works.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assert pre-populated values are set into the thread context.
        assertEquals(requestId, ThreadLocalContextStorage.get(FILIBUSTER_REQUEST_ID));
        assertEquals(distributedExecutionIndexStr, ThreadLocalContextStorage.get(FILIBUSTER_EXECUTION_INDEX));
        assertEquals(vclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_VCLOCK)).toString());
        assertEquals(originVclock.toString(), Objects.requireNonNull(ThreadLocalContextStorage.get(FILIBUSTER_ORIGIN_VCLOCK)).toString());
    }
}
