package cloud.filibuster.instrumentation.libraries.armeria.http.tests.available;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.armeria.http.tests.FilibusterDecoratingHttpClientTest;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_VCLOCK;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilibusterDecoratingHttpClientToAvailableServiceTest extends FilibusterDecoratingHttpClientTest {
    private final String baseExternalURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";

    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startFilibuster();
        startExternalServer();
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopFilibuster();
        stopExternalServer();
    }

    @AfterEach
    public void resetFilibusterConfiguration() {
        FilibusterServer.shouldReturnNotFounds = false;
        FilibusterServer.noNewTestExecution = false;
        FilibusterDecoratingHttpClient.disableServerCommunication = false;
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (no headers, first request.)")
    public void testClientDecoratorToAvailableServiceNoHeaders() {
        String serviceName = "hello";

        setInitialVectorClock(null);
        setInitialOriginVectorClock(null);
        setInitialDistributedExecutionIndex(null);

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("hello-FilibusterDecoratingHttpClientToAvailableServiceTest.java-67-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        // Success values.
        JSONObject returnValue = lastPayload.getJSONObject("return_value");
        assertEquals("200", returnValue.getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", returnValue.getString("__class__"));

        // There should only be a single key; verify the execution index.
        DistributedExecutionIndex distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(createNewDistributedExecutionIndex().toString(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        VectorClock finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(ThreadLocalContextStorage.get(FILIBUSTER_VCLOCK), finalVectorClock));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service.")
    public void testClientDecoratorToAvailableService() {
        String serviceName = "hello";

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("hello-FilibusterDecoratingHttpClientToAvailableServiceTest.java-109-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        // Success values.
        JSONObject returnValue = lastPayload.getJSONObject("return_value");
        assertEquals("200", returnValue.getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", returnValue.getString("__class__"));

        // There should only be a single key; verify the execution index.
        DistributedExecutionIndex distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(getInitialDistributedExecutionIndex(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        VectorClock finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(getInitialVectorClock(), finalVectorClock));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with instrumentation disabled.)")
    public void testClientDecoratorToAvailableServiceWithInstrumentationDisabled() {
        String serviceName = "hello";

        FilibusterDecoratingHttpClient.disableServerCommunication = true;

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // There should only be a single key; verify the execution index.
        DistributedExecutionIndex distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(getInitialDistributedExecutionIndex(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        VectorClock finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(getInitialVectorClock(), finalVectorClock));

        // Nothing should be logged.
        assertEquals(0, FilibusterServer.payloadsReceived.size());
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (two requests, sequence.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsInSequence() {
        String serviceName = "hello";

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock(serviceName);
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("hello-FilibusterDecoratingHttpClientToAvailableServiceTest.java-181-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        // Success values.
        JSONObject returnValue = lastPayload.getJSONObject("return_value");
        assertEquals("200", returnValue.getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", returnValue.getString("__class__"));

        // There should only be a single key; verify the execution index.
        DistributedExecutionIndex distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(getInitialDistributedExecutionIndex(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        VectorClock finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(getInitialVectorClock(), finalVectorClock));

        // **************************************************************************************
        // Second request.
        // **************************************************************************************

        FilibusterServer.noNewTestExecution = true;

        webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        response = webClient.execute(getHeaders).aggregate().join();
        headers = response.headers();
        statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        assertVc = new VectorClock();
        assertVc.incrementClock(serviceName);
        assertVc.incrementClock(serviceName);
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("hello-FilibusterDecoratingHttpClientToAvailableServiceTest.java-224-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        // Success values.
        returnValue = lastPayload.getJSONObject("return_value");
        assertEquals("200", returnValue.getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", returnValue.getString("__class__"));

        // There should only be a single key; verify the execution index.
        distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(getInitialDistributedExecutionIndex(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(getInitialVectorClock(), finalVectorClock));
    }

    @Test
    @DisplayName("Test a broken Filibuster.")
    public void testClientDecoratorWithBrokenFilibuster() {
        String serviceName = "hello";

        FilibusterServer.shouldReturnNotFounds = true;

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Nothing should be logged.
        assertEquals(0, FilibusterServer.payloadsReceived.size());
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service with TLS.")
    public void testClientDecoratorToAvailableServiceWithTLS() {
        String serviceName = "hello";

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("hello-FilibusterDecoratingHttpClientToAvailableServiceTest.java-287-WebClient-GET-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        // Success values.
        JSONObject returnValue = lastPayload.getJSONObject("return_value");
        assertEquals("200", returnValue.getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", returnValue.getString("__class__"));

        // There should only be a single key; verify the execution index.
        DistributedExecutionIndex distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(getInitialDistributedExecutionIndex(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        VectorClock finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(getInitialVectorClock(), finalVectorClock));
    }
}
