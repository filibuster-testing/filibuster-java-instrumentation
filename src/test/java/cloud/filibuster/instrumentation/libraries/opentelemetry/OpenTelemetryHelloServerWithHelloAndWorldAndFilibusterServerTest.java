package cloud.filibuster.instrumentation.libraries.opentelemetry;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.examples.armeria.http.tests.HelloServerTest;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cloud.filibuster.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenTelemetryHelloServerWithHelloAndWorldAndFilibusterServerTest extends HelloServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        super.startHelloServer();
        super.startWorldServer();
        super.startExternalServer();
        super.startFilibuster();

        FilibusterServer.oneNewTestExecution = true;
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        super.stopHelloServer();
        super.stopWorldServer();
        super.stopExternalServer();
        super.stopFilibuster();

        FilibusterServer.noNewTestExecution = false;

        resetInitialDistributedExecutionIndex();
    }

    @BeforeEach
    public void enableFilibuster() {
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
        FilibusterDecoratingHttpService.disableInstrumentation = false;
    }

    // This tests services using mixed styles of instrumentation: one with otel the other with manual and
    // thread locals for HTTP.
    @Test
    @DisplayName("Test hello server world otel external route (with Filibuster.)")
    public void testWorldOtelExternalWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world-otel-external", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assemble execution index.
        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("hello-HelloServer.java-554-WebClient-GET-0b2c7a3d6d82ede9ae2958a787ce2639af116476-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestDistributedExecutionIndex.push("hello-HelloServer.java-554-WebClient-GET-0b2c7a3d6d82ede9ae2958a787ce2639af116476-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestDistributedExecutionIndex.push("world-WorldServer.java-81-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.
        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // Very proper number of Filibuster records.
        assertEquals(5, FilibusterServer.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
    }

    // This tests end-to-end otel instrumentation for HTTP.
    @Test
    @DisplayName("Test hello server world otel external otel route (with Filibuster.)")
    public void testWorldOtelExternalOtelWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world-otel-external-otel", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assemble execution index.
        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("hello-HelloServer.java-583-WebClient-GET-1a01d86894fc54286fb250d8dc0cf83a28aac139-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestDistributedExecutionIndex.push("hello-HelloServer.java-583-WebClient-GET-1a01d86894fc54286fb250d8dc0cf83a28aac139-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestDistributedExecutionIndex.push("world-WorldServer.java-108-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.
        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // Very proper number of Filibuster records.
        assertEquals(5, FilibusterServer.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
    }
}
