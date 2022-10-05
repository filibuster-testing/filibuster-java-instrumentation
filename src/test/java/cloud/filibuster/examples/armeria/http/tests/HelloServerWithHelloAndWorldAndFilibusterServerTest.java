package cloud.filibuster.examples.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
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
import static cloud.filibuster.examples.test_servers.HelloServer.setInitialDistributedExecutionIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloServerWithHelloAndWorldAndFilibusterServerTest extends HelloServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        super.startHelloServer();
        super.startWorldServer();
        super.startExternalServer();
        super.startFilibuster();

        FilibusterServer.oneNewTestExecution = true;

        DistributedExecutionIndex startingDistributedExecutionIndex = createNewDistributedExecutionIndex();
        startingDistributedExecutionIndex.push("some-random-location-1337");

        setInitialDistributedExecutionIndex(startingDistributedExecutionIndex.toString());
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

    @Test
    @DisplayName("Test hello server world route (with Filibuster.)")
    public void testWorldWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assemble execution index.
        DistributedExecutionIndex requestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        requestDistributedExecutionIndex.push("some-random-location-1337");
        requestDistributedExecutionIndex.push("hello-HelloServer.java-446-WebClient-GET-b4604597df48b0eae13cf0c110562e150ace79ff-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Very proper number of Filibuster records.
        assertEquals(3, FilibusterServer.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject invocationCompletedPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", invocationCompletedPayload.getString("instrumentation_type"));
        assertEquals(0, invocationCompletedPayload.getInt("generated_id"));
        assertEquals(requestDistributedExecutionIndex.toString(), invocationCompletedPayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test hello server world twice route (with Filibuster.)")
    public void testWorldTwiceWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world-twice", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Very proper number of Filibuster records.
        assertEquals(6, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("some-random-location-1337");
        firstRequestDistributedExecutionIndex.push("hello-HelloServer.java-475-WebClient-GET-b4604597df48b0eae13cf0c110562e150ace79ff-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestDistributedExecutionIndex.push("some-random-location-1337");
        secondRequestDistributedExecutionIndex.push("hello-HelloServer.java-491-WebClient-GET-b4604597df48b0eae13cf0c110562e150ace79ff-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationCompletedPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServer.payloadsReceived.get(5);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationCompletedPayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test hello server cycle route (with Filibuster.)")
    public void testCycleWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/cycle", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Very proper number of Filibuster records.
        assertEquals(6, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("some-random-location-1337");
        firstRequestDistributedExecutionIndex.push("hello-HelloServer.java-612-WebClient-GET-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestDistributedExecutionIndex.push("some-random-location-1337");
        secondRequestDistributedExecutionIndex.push("hello-HelloServer.java-612-WebClient-GET-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestDistributedExecutionIndex.push("world-WorldServer.java-135-WebClient-GET-f05ca0f49ff94c87bb10dd4265940b0bba02dcb2-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstRequestReceivedPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondRequestReceivedPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(5);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());
    }

    @Test
    @DisplayName("Test hello server cycle 2 route (with Filibuster.)")
    public void testCycle2WithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/cycle2", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Very proper number of Filibuster records.
        assertEquals(9, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("some-random-location-1337");
        firstRequestDistributedExecutionIndex.push("hello-HelloServer.java-641-WebClient-GET-cb508e66d0f8ee1ef4546567356eb01c51cb9598-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestDistributedExecutionIndex.push("some-random-location-1337");
        secondRequestDistributedExecutionIndex.push("hello-HelloServer.java-641-WebClient-GET-cb508e66d0f8ee1ef4546567356eb01c51cb9598-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestDistributedExecutionIndex.push("world-WorldServer.java-162-WebClient-GET-44f8aba85b6dc43f841d4ae2648ad1c7a922a331-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex thirdRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        thirdRequestDistributedExecutionIndex.push("some-random-location-1337");
        thirdRequestDistributedExecutionIndex.push("hello-HelloServer.java-641-WebClient-GET-cb508e66d0f8ee1ef4546567356eb01c51cb9598-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        thirdRequestDistributedExecutionIndex.push("world-WorldServer.java-162-WebClient-GET-44f8aba85b6dc43f841d4ae2648ad1c7a922a331-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        thirdRequestDistributedExecutionIndex.push("hello-HelloServer.java-525-WebClient-GET-b4604597df48b0eae13cf0c110562e150ace79ff-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        VectorClock thirdRequestVectorClock = new VectorClock();
        thirdRequestVectorClock.incrementClock("hello");
        thirdRequestVectorClock.incrementClock("world");
        thirdRequestVectorClock.incrementClock("hello");

        // These are the required Filibuster instrumentation fields for this call.

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

        JSONObject secondRequestReceivedPayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationPayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation", thirdInvocationPayload.getString("instrumentation_type"));
        assertEquals(thirdRequestDistributedExecutionIndex.toString(), thirdInvocationPayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationPayload.getJSONObject("vclock").toString());

        JSONObject thirdRequestReceivedPayload = FilibusterServer.payloadsReceived.get(5);
        assertEquals("request_received", thirdRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(thirdRequestDistributedExecutionIndex.toString(), thirdRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationCompletePayload = FilibusterServer.payloadsReceived.get(6);
        assertEquals("invocation_complete", thirdInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(thirdRequestDistributedExecutionIndex.toString(), thirdInvocationCompletePayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(7);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(8);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());
    }

    @Test
    @DisplayName("Test hello server cycle 3 route (with Filibuster.)")
    public void testCycle3WithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/cycle3", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Very proper number of Filibuster records.
        assertEquals(12, FilibusterServer.payloadsReceived.size());

        // Assemble execution indexes.

        DistributedExecutionIndex firstRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        firstRequestDistributedExecutionIndex.push("some-random-location-1337");
        firstRequestDistributedExecutionIndex.push("hello-HelloServer.java-670-WebClient-GET-cb508e66d0f8ee1ef4546567356eb01c51cb9598-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex secondRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        secondRequestDistributedExecutionIndex.push("some-random-location-1337");
        secondRequestDistributedExecutionIndex.push("hello-HelloServer.java-670-WebClient-GET-cb508e66d0f8ee1ef4546567356eb01c51cb9598-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        secondRequestDistributedExecutionIndex.push("world-WorldServer.java-162-WebClient-GET-44f8aba85b6dc43f841d4ae2648ad1c7a922a331-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex thirdRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        thirdRequestDistributedExecutionIndex.push("some-random-location-1337");
        thirdRequestDistributedExecutionIndex.push("hello-HelloServer.java-670-WebClient-GET-cb508e66d0f8ee1ef4546567356eb01c51cb9598-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        thirdRequestDistributedExecutionIndex.push("world-WorldServer.java-162-WebClient-GET-44f8aba85b6dc43f841d4ae2648ad1c7a922a331-da39a3ee5e6b4b0d3255bfef95601890afd80709");
        thirdRequestDistributedExecutionIndex.push("hello-HelloServer.java-525-WebClient-GET-b4604597df48b0eae13cf0c110562e150ace79ff-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        DistributedExecutionIndex fourthRequestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        fourthRequestDistributedExecutionIndex.push("some-random-location-1337");
        fourthRequestDistributedExecutionIndex.push("hello-HelloServer.java-685-WebClient-GET-b4604597df48b0eae13cf0c110562e150ace79ff-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        VectorClock thirdRequestVectorClock = new VectorClock();
        thirdRequestVectorClock.incrementClock("hello");
        thirdRequestVectorClock.incrementClock("world");
        thirdRequestVectorClock.incrementClock("hello");

        VectorClock fourthRequestVectorClock = new VectorClock();
        fourthRequestVectorClock.incrementClock("hello");
        fourthRequestVectorClock.incrementClock("hello");

        // These are the required Filibuster instrumentation fields for this call.

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

        JSONObject secondRequestReceivedPayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationPayload = FilibusterServer.payloadsReceived.get(4);
        assertEquals("invocation", thirdInvocationPayload.getString("instrumentation_type"));
        assertEquals(thirdRequestDistributedExecutionIndex.toString(), thirdInvocationPayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationPayload.getJSONObject("vclock").toString());

        JSONObject thirdRequestReceivedPayload = FilibusterServer.payloadsReceived.get(5);
        assertEquals("request_received", thirdRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(thirdRequestDistributedExecutionIndex.toString(), thirdRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationCompletePayload = FilibusterServer.payloadsReceived.get(6);
        assertEquals("invocation_complete", thirdInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(thirdRequestDistributedExecutionIndex.toString(), thirdInvocationCompletePayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServer.payloadsReceived.get(7);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(secondRequestDistributedExecutionIndex.toString(), secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServer.payloadsReceived.get(8);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(firstRequestDistributedExecutionIndex.toString(), firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject fourthInvocationPayload = FilibusterServer.payloadsReceived.get(9);
        assertEquals("invocation", fourthInvocationPayload.getString("instrumentation_type"));
        assertEquals(fourthRequestDistributedExecutionIndex.toString(), fourthInvocationPayload.getString("execution_index"));
        assertEquals(fourthRequestVectorClock.toJSONObject().toString(), fourthInvocationPayload.getJSONObject("vclock").toString());

        JSONObject fourthRequestReceivedPayload = FilibusterServer.payloadsReceived.get(10);
        assertEquals("request_received", fourthRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals(fourthRequestDistributedExecutionIndex.toString(), fourthRequestReceivedPayload.getString("execution_index"));

        JSONObject fourthInvocationCompletePayload = FilibusterServer.payloadsReceived.get(11);
        assertEquals("invocation_complete", fourthInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals(fourthRequestDistributedExecutionIndex.toString(), fourthInvocationCompletePayload.getString("execution_index"));
        assertEquals(fourthRequestVectorClock.toJSONObject().toString(), fourthInvocationCompletePayload.getJSONObject("vclock").toString());
    }

    @Test
    @DisplayName("Test hello server external POST route (with Filibuster.)")
    public void testExternalPostWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/external-post", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assemble execution index.
        DistributedExecutionIndex requestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        requestDistributedExecutionIndex.push("some-random-location-1337");
        requestDistributedExecutionIndex.push("hello-HelloServer.java-723-WebClient-POST-2d1ef8410983c2242adbaa0457f03575b8c06b92-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Very proper number of Filibuster records.
        assertEquals(2, FilibusterServer.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject invocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", invocationPayload.getString("instrumentation_type"));
        assertEquals(requestDistributedExecutionIndex.toString(), invocationPayload.getString("execution_index"));

        JSONObject invocationCompletePayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation_complete", invocationCompletePayload.getString("instrumentation_type"));
        assertEquals(requestDistributedExecutionIndex.toString(), invocationCompletePayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test hello server external PUT route (with Filibuster.)")
    public void testExternalPutWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/external-put", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Assemble execution index.
        DistributedExecutionIndex requestDistributedExecutionIndex = createNewDistributedExecutionIndex();
        requestDistributedExecutionIndex.push("some-random-location-1337");
        requestDistributedExecutionIndex.push("hello-HelloServer.java-755-WebClient-PUT-a547a5665dc2aca5823133e2152f4333fb9a36cc-da39a3ee5e6b4b0d3255bfef95601890afd80709");

        // Very proper number of Filibuster records.
        assertEquals(2, FilibusterServer.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject invocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", invocationPayload.getString("instrumentation_type"));
        assertEquals(requestDistributedExecutionIndex.toString(), invocationPayload.getString("execution_index"));

        JSONObject invocationCompletePayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation_complete", invocationCompletePayload.getString("instrumentation_type"));
        assertEquals(requestDistributedExecutionIndex.toString(), invocationCompletePayload.getString("execution_index"));
    }
}


