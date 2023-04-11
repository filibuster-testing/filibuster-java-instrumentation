package cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.available;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.FilibusterDecoratingHttpClientTest;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_VCLOCK;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilibusterDecoratingHttpClientToAvailableServiceTest extends FilibusterDecoratingHttpClientTest {
    private final String baseExternalURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";

    @BeforeAll
    public static void startServices() throws IOException, InterruptedException {
        startFilibuster();
        startExternalServer();
    }

    @AfterAll
    public static void stopServices() throws InterruptedException {
        stopFilibuster();
        stopExternalServer();
    }

    @AfterEach
    public void resetFilibusterConfiguration() {
        FilibusterServerFake.shouldReturnNotFounds = false;
        FilibusterServerFake.noNewTestExecution = false;
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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-39d6698f977453d22bd630bca3a0cd94163c0d12-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));

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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-ad52c61bc9b8b79b252be451d33d34c267fbad44-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));

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
        assertEquals(0, FilibusterServerFake.payloadsReceived.size());
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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock(serviceName);
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-312fe226bd92b74e2448ed918aef06313dffaf71-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));

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

        FilibusterServerFake.noNewTestExecution = true;

        webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        response = webClient.execute(getHeaders).aggregate().join();
        headers = response.headers();
        statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        assertVc = new VectorClock();
        assertVc.incrementClock(serviceName);
        assertVc.incrementClock(serviceName);
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-20c6ab526453917f21028d73f036ec62f82b4daf-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));

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

        FilibusterServerFake.shouldReturnNotFounds = true;

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Nothing should be logged.
        assertEquals(0, FilibusterServerFake.payloadsReceived.size());
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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-b2ad3926312b1ffbbf0549f78794f7ed34c5fc4f-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));

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
