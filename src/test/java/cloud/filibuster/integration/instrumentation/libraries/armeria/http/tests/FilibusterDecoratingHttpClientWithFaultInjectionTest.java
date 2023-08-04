package cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
        FilibusterServerFake.emptyExceptionString = false;
        FilibusterServerFake.emptyExceptionCauseString = false;
        FilibusterServerFake.shouldInjectStatusCodeFault = false;
        FilibusterServerFake.shouldNotAbort = false;
        FilibusterServerFake.shouldInjectExceptionFault = false;
        FilibusterServerFake.skipSleepKey = false;
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (exception.)")
    public void testClientDecoratorWithFaultInjectionExceptionToAvailableService() {
        FilibusterServerFake.shouldInjectExceptionFault = true;

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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e92305b2d58e31e089199e09a098fc247c5257ae-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));
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
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.emptyExceptionString = true;

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
        assertTrue(exception.getCause() instanceof FilibusterFaultInjectionException);
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (empty exceptionCauseString.)")
    public void testClientDecoratorWithFaultInjectionToAvailableServiceEmptyExceptionCauseString() {
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.emptyExceptionCauseString = true;

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
        assertTrue(exception.getCause() instanceof FilibusterFaultInjectionException);
    }

    @Test
    @DisplayName("Test Filibuster fault injection to available service (status code.)")
    public void testClientDecoratorWithFaultInjectionStatusCodeToAvailableService() {
        FilibusterServerFake.shouldInjectStatusCodeFault = true;

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, "hello");
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("404", statusCode);

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-5d22ad26da88f88b9834ac059a44a7b9ed001c97-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertEquals("404", lastPayload.getJSONObject("return_value").getString("status_code"));
        assertEquals("com.linecorp.armeria.common.HttpResponse", lastPayload.getJSONObject("return_value").getString("__class__"));
    }

    @Test
    @DisplayName("Test Filibuster fault injection to unresolvable service (exception.)")
    public void testClientDecoratorWithFaultInjectionExceptionToUnresolvableService() {
        FilibusterServerFake.shouldInjectExceptionFault = true;

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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-5d645721ba5178fadc706deecf8bdc1cc4ced151-ac033f1816b49895f7f2585389d6dcf15d32e359\", 1]]", lastPayload.getString("execution_index"));
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
        FilibusterServerFake.skipSleepKey = true;
        FilibusterServerFake.shouldInjectExceptionFault = true;

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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-2ae3bb7f18cb9349ed89f20522420f3a62029480-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", lastPayload.getString("execution_index"));
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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));

        VectorClock assertVc = new VectorClock();
        assertVc.incrementClock("hello");
        assertEquals(assertVc.toString(), lastPayload.get("vclock").toString());

        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-256782bc886679cd3bbd85cd41c6767ce6074577-e205a3212a4bfa2c8be52e7b918f526507eef179\", 1]]", lastPayload.getString("execution_index"));

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
        FilibusterServerFake.shouldInjectExceptionFault = true;
        FilibusterServerFake.shouldNotAbort = true;

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
        JSONObject lastPayload = FilibusterServerFake.payloadsReceived.get(FilibusterServerFake.payloadsReceived.size() - 1);
        assertEquals("invocation_complete", lastPayload.getString("instrumentation_type"));
        assertEquals(0, lastPayload.getInt("generated_id"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-db082cf8db1dc42c9c3f534ca27df61d9d002893-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-cfdb81cf1f4ffac52228e7d28d025bf59bf49638-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", lastPayload.getString("execution_index"));
        VectorClock assertVectorClock = new VectorClock();
        assertVectorClock.incrementClock("hello");
        assertEquals(assertVectorClock.toString(), lastPayload.getJSONObject("vclock").toString());
        assertEquals(0, lastPayload.getJSONObject("exception").getJSONObject("metadata").getInt("sleep"));
        assertFalse(lastPayload.getJSONObject("exception").getJSONObject("metadata").getBoolean("abort"));
        assertEquals("io.netty.channel.ConnectTimeoutException", lastPayload.getJSONObject("exception").getJSONObject("metadata").getString("cause"));
        assertEquals("com.linecorp.armeria.client.UnprocessedRequestException", lastPayload.getJSONObject("exception").getString("name"));
    }
}
