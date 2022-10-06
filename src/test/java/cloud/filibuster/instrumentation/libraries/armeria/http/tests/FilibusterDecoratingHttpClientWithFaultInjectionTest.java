package cloud.filibuster.instrumentation.libraries.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;

import cloud.filibuster.instrumentation.helpers.Networking;
import com.linecorp.armeria.client.UnprocessedRequestException;
import com.linecorp.armeria.client.WebClient;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import io.netty.channel.ConnectTimeoutException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilibusterDecoratingHttpClientWithFaultInjectionTest extends FilibusterDecoratingHttpClientTest {
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
        FilibusterServer.emptyExceptionString = false;
        FilibusterServer.emptyExceptionCauseString = false;
        FilibusterServer.shouldInjectStatusCodeFault = false;
        FilibusterServer.shouldNotAbort = false;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.skipSleepKey = false;
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (exception.)")
    public void testClientDecoratorWithFaultInjectionExceptionToAvailableService() {
        FilibusterServer.shouldInjectExceptionFault = true;

        Exception exception = null;

        try {
            WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        } catch (RuntimeException e) {
            exception = e;
        }

        assertTrue(exception instanceof CompletionException);
        assertTrue(exception.getCause() instanceof UnprocessedRequestException);
        assertTrue(exception.getCause().getCause() instanceof ConnectTimeoutException);
        String message = exception.getCause().getCause().getMessage();
        assertTrue(message.matches("connection timed out: 0.0.0.0/0.0.0.0:5004"));

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f9ca9f28fae1fae27785f3b41678b2712637fea1-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertEquals(0, lastPayload.getJSONObject("exception").getJSONObject("metadata").getInt("sleep"));
        assertTrue(lastPayload.getJSONObject("exception").getJSONObject("metadata").getBoolean("abort"));
        assertEquals("io.netty.channel.ConnectTimeoutException", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("cause"));
        assertEquals("com.linecorp.armeria.client.UnprocessedRequestException", lastPayload.getJSONObject("exception").getString("name"));
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (empty exceptionString.)")
    public void testClientDecoratorWithFaultInjectionToAvailableServiceEmptyExceptionString() {
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.emptyExceptionString = true;

        Exception exception = null;

        try {
            WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        } catch (RuntimeException e) {
            exception = e;
        }

        assertTrue(exception instanceof CompletionException);
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (empty exceptionCauseString.)")
    public void testClientDecoratorWithFaultInjectionToAvailableServiceEmptyExceptionCauseString() {
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.emptyExceptionCauseString = true;

        Exception exception = null;

        try {
            WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        } catch (RuntimeException e) {
            exception = e;
        }

        assertTrue(exception instanceof CompletionException);
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (status code.)")
    public void testClientDecoratorWithFaultInjectionStatusCodeToAvailableService() {
        FilibusterServer.shouldInjectStatusCodeFault = true;

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("404", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-21572ad7301668b439a42c33ab33b11ab3763b5f-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertEquals("404", lastPayload.getJSONObject("return_value").getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", lastPayload.getJSONObject("return_value").getString("__class__"));
    }

    @Test
    @DisplayName("Test Filibuster fault injection to unresolvable service (exception.)")
    public void testClientDecoratorWithFaultInjectionExceptionToUnresolvableService() {
        FilibusterServer.shouldInjectExceptionFault = true;

        Exception exception = null;

        try {
            WebClient webClient = TestHelper.getTestWebClient("http://asedf.cloud", "hello");
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        } catch (RuntimeException e) {
            exception = e;
        }

        assertTrue(exception instanceof CompletionException);
        assertTrue(exception.getCause() instanceof UnprocessedRequestException);
        assertTrue(exception.getCause().getCause() instanceof ConnectTimeoutException);
        String message = exception.getCause().getCause().getMessage();
        assertTrue(message.matches("connection timed out: asedf.cloud/.*:80"));

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f7930151f242562eea880f5cef7f62507dd8c02c-07b3a2342a2737389063df8ce7dc601bb7d1b740");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertEquals(0, lastPayload.getJSONObject("exception").getJSONObject("metadata").getInt("sleep"));
        assertTrue(lastPayload.getJSONObject("exception").getJSONObject("metadata").getBoolean("abort"));
        assertEquals("io.netty.channel.ConnectTimeoutException", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("cause"));
        assertEquals("com.linecorp.armeria.client.UnprocessedRequestException", lastPayload.getJSONObject("exception").getString("name"));
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (no sleep key.)")
    public void testClientDecoratorWithFaultInjectionExceptionToAvailableServiceNoSleepKey() {
        FilibusterServer.skipSleepKey = true;
        FilibusterServer.shouldInjectExceptionFault = true;

        Exception exception = null;

        try {
            WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        } catch (RuntimeException e) {
            exception = e;
        }

        assertTrue(exception instanceof CompletionException);
        assertTrue(exception.getCause() instanceof UnprocessedRequestException);
        assertTrue(exception.getCause().getCause() instanceof ConnectTimeoutException);
        String message = exception.getCause().getCause().getMessage();
        assertTrue(message.matches("connection timed out: 0.0.0.0/0.0.0.0:5004"));

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e89af889a40ed4e288e04b3934087b3f9704cf64-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertFalse(lastPayload.getJSONObject("exception").getJSONObject("metadata").has("sleep"));
        assertTrue(lastPayload.getJSONObject("exception").getJSONObject("metadata").getBoolean("abort"));
        assertEquals("io.netty.channel.ConnectTimeoutException", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("cause"));
        assertEquals("com.linecorp.armeria.client.UnprocessedRequestException", lastPayload.getJSONObject("exception").getString("name"));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with failure.)")
    public void testClientDecoratorToAvailableServiceWithFailure() {
        String serviceName = "hello";

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI + "404", serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("404", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-1f54ca3c33f9907715cc85c45f1985907daedf89-eb7ad95489a301d5b8072cf506df6a9d34b0b22c");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));

        // Success values.
        JSONObject returnValue = lastPayload.getJSONObject("return_value");
        assertEquals("404", returnValue.getString("status_code"));
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
    @DisplayName("Test Filibuster fault injection to available service (exception with false abort.)")
    public void testClientDecoratorWithFaultInjectionExceptionToAvailableServiceWithFalseAbort() {
        FilibusterServer.shouldInjectExceptionFault = true;
        FilibusterServer.shouldNotAbort = true;

        Exception exception = null;

        try {
            WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        } catch (RuntimeException e) {
            exception = e;
        }

        assertTrue(exception instanceof CompletionException);
        assertTrue(exception.getCause() instanceof UnprocessedRequestException);
        assertTrue(exception.getCause().getCause() instanceof ConnectTimeoutException);
        String message = exception.getCause().getCause().getMessage();
        assertTrue(message.matches("connection timed out: 0.0.0.0/0.0.0.0:5004"));

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServer.payloadsReceived.get(FilibusterServer.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        DistributedExecutionIndex assertDistributedExecutionIndex = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndex.push("chris");
        assertDistributedExecutionIndex.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e942718547c329c3e1a7b421c857f2bf726a1ed1-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndex.toString(), lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertEquals(0, lastPayload.getJSONObject("exception").getJSONObject("metadata").getInt("sleep"));
        assertFalse(lastPayload.getJSONObject("exception").getJSONObject("metadata").getBoolean("abort"));
        assertEquals("io.netty.channel.ConnectTimeoutException", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("cause"));
        assertEquals("com.linecorp.armeria.client.UnprocessedRequestException", lastPayload.getJSONObject("exception").getString("name"));
    }
}
