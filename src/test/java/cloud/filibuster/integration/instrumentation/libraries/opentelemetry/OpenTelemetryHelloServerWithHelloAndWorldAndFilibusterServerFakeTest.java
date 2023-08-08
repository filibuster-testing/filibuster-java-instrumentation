package cloud.filibuster.integration.instrumentation.libraries.opentelemetry;

import cloud.filibuster.integration.examples.armeria.http.tests.HelloServerTest;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cloud.filibuster.integration.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenTelemetryHelloServerWithHelloAndWorldAndFilibusterServerFakeTest extends HelloServerTest {
    @BeforeAll
    public static void startServices() throws IOException, InterruptedException {
        startHelloServer();
        startWorldServer();
        startExternalServer();
        startFilibuster();
    }

    @AfterAll
    public static void stopServices() throws InterruptedException {
        stopHelloServer();
        stopWorldServer();
        stopExternalServer();
        stopFilibuster();
    }

    @BeforeEach
    public void resetConfigurationBeforeAll() {
        FilibusterServerFake.oneNewTestExecution = true;
        FilibusterServerFake.resetPayloadsReceived();
    }

    @AfterEach
    public void resetConfigurationAfterAll() {
        FilibusterServerFake.noNewTestExecution = false;
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

        // Assemble vector clocks.
        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // Very proper number of Filibuster records.
        assertEquals(5, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a705ed5b37a7ad1a90cc64c6547ed1e178a9b3e3-cd553bcb135254a6e404722518353bba5cd1c7ca\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a705ed5b37a7ad1a90cc64c6547ed1e178a9b3e3-cd553bcb135254a6e404722518353bba5cd1c7ca\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a705ed5b37a7ad1a90cc64c6547ed1e178a9b3e3-cd553bcb135254a6e404722518353bba5cd1c7ca\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-c5243e30b91ebfb78127957601f1e732065c0712-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a705ed5b37a7ad1a90cc64c6547ed1e178a9b3e3-cd553bcb135254a6e404722518353bba5cd1c7ca\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-c5243e30b91ebfb78127957601f1e732065c0712-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a705ed5b37a7ad1a90cc64c6547ed1e178a9b3e3-cd553bcb135254a6e404722518353bba5cd1c7ca\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
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

        // Assemble vector clocks.
        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // Very proper number of Filibuster records.
        assertEquals(5, FilibusterServerFake.payloadsReceived.size());

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bff96a2a30981288fd5bcd38cfa72adcd6839f87-7bcdb88aab6e3f3843fcdbf587c4e5e56f34c6c2\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bff96a2a30981288fd5bcd38cfa72adcd6839f87-7bcdb88aab6e3f3843fcdbf587c4e5e56f34c6c2\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bff96a2a30981288fd5bcd38cfa72adcd6839f87-7bcdb88aab6e3f3843fcdbf587c4e5e56f34c6c2\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-60c35754561025cfd6a26701f03f4dcdeedfe6d3-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bff96a2a30981288fd5bcd38cfa72adcd6839f87-7bcdb88aab6e3f3843fcdbf587c4e5e56f34c6c2\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-60c35754561025cfd6a26701f03f4dcdeedfe6d3-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bff96a2a30981288fd5bcd38cfa72adcd6839f87-7bcdb88aab6e3f3843fcdbf587c4e5e56f34c6c2\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
    }
}
