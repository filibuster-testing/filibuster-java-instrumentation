package cloud.filibuster.instrumentation.libraries.armeria.http.tests.available;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.tests.FilibusterDecoratingHttpClientTest;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilibusterDecoratingHttpClientToAvailableServiceDistributedExecutionIndexTest extends FilibusterDecoratingHttpClientTest {
    @BeforeEach
    public void noNewTestExecution() {
        FilibusterServer.noNewTestExecution = true;
    }

    @AfterEach
    public void newTestExecution() {
        FilibusterServer.noNewTestExecution = false;
    }

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
    public void resetThreadContextState() {
        resetInitialDistributedExecutionIndex();
        resetInitialRequestId();
        resetInitialOriginVectorClock();
        resetInitialVectorClock();
    }

    private final String baseExternalURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (no headers, two futures.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsNoHeaders() {
        setInitialVectorClock(null);
        setInitialOriginVectorClock(null);
        setInitialDistributedExecutionIndex(null);
        setInitialRequestId("1");

        // Setup request 1.
        WebClient webClient1 = TestHelper.getTestWebClient(baseExternalURI, "hello");
        RequestHeaders getHeaders1 = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

        // Setup request 2.
        WebClient webClient2 = TestHelper.getTestWebClient(baseExternalURI, "hello");
        RequestHeaders getHeaders2 = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

        // Issue request 1.
        HttpResponse response1 = webClient1.execute(getHeaders1);

        // Issue request 2.
        HttpResponse response2 = webClient2.execute(getHeaders2);

        // Complete 1.
        AggregatedHttpResponse aggregatedHttpResponse1 = response1.aggregate().join();
        ResponseHeaders headers1 = aggregatedHttpResponse1.headers();
        assertEquals("200", headers1.get(HttpHeaderNames.STATUS));

        // Complete 2.
        AggregatedHttpResponse aggregatedHttpResponse2 = response2.aggregate().join();
        ResponseHeaders headers2 = aggregatedHttpResponse2.headers();
        assertEquals("200", headers2.get(HttpHeaderNames.STATUS));

        assertEquals(4, FilibusterServer.payloadsReceived.size());

        // Verify invocation for first request.
        JSONObject webClient1InvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", webClient1InvocationPayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationWebClient1 = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndexInvocationWebClient1.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f25bed509379434f03d62c7a78b1f33e950b86a0-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationWebClient1.toString(), webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationWebClient2 = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndexInvocationWebClient2.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bf2e9d84c7b21bfc9cab1620c54597e2738eb110-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationWebClient2.toString(), webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationCompleteWebClient1 = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndexInvocationCompleteWebClient1.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f25bed509379434f03d62c7a78b1f33e950b86a0-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationCompleteWebClient1.toString(), webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationCompleteWebClient2 = createNewDistributedExecutionIndex();
        assertDistributedExecutionIndexInvocationCompleteWebClient2.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-bf2e9d84c7b21bfc9cab1620c54597e2738eb110-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationCompleteWebClient2.toString(), webClient2InvocationCompletePayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (no headers, two futures, in loop.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsNoHeadersInLoop() {
        setInitialVectorClock(null);
        setInitialOriginVectorClock(null);
        setInitialDistributedExecutionIndex(null);
        setInitialRequestId("1");

        // Verify each iteration of the loop to ensure EI properly updates.
        for(int i = 0; i < 2; i++) {
            WebClient webClient3 = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders3 = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

            HttpResponse response3 = webClient3.execute(getHeaders3);

            AggregatedHttpResponse aggregatedHttpResponse3 = response3.aggregate().join();
            ResponseHeaders headers3 = aggregatedHttpResponse3.headers();
            assertEquals("200", headers3.get(HttpHeaderNames.STATUS));

            int invocationEntry = FilibusterServer.payloadsReceived.size() - 2;
            JSONObject webClient3InvocationPayload = FilibusterServer.payloadsReceived.get(invocationEntry);
            assertEquals("invocation", webClient3InvocationPayload.getString("instrumentation_type"));

            DistributedExecutionIndex assertDistributedExecutionIndexForWebClient3 = createNewDistributedExecutionIndex();
            assertDistributedExecutionIndexForWebClient3.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aecd0e34ab7486b22bcc6b5eb4cba7f4bfd331b9-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
            if (i == 1) {
                assertDistributedExecutionIndexForWebClient3.pop();
                assertDistributedExecutionIndexForWebClient3.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aecd0e34ab7486b22bcc6b5eb4cba7f4bfd331b9-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
            }
            assertEquals(assertDistributedExecutionIndexForWebClient3.toString(), webClient3InvocationPayload.getString("execution_index"));

            int invocationCompleteEntry = FilibusterServer.payloadsReceived.size() - 1;
            JSONObject webClient3InvocationCompletePayload = FilibusterServer.payloadsReceived.get(invocationCompleteEntry);
            assertEquals("invocation_complete", webClient3InvocationCompletePayload.getString("instrumentation_type"));
        }
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with headers, two futures.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsWithHeaders() {
        DistributedExecutionIndex startingExecutionIndex = createNewDistributedExecutionIndex();
        startingExecutionIndex.push("some-random-location-1337");

        setInitialVectorClock(null);
        setInitialOriginVectorClock(null);
        setInitialDistributedExecutionIndex(startingExecutionIndex.toString());
        setInitialRequestId("2");

        // Setup request 1.
        WebClient webClient1 = TestHelper.getTestWebClient(baseExternalURI, "hello");
        RequestHeaders getHeaders1 = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

        // Setup request 2.
        WebClient webClient2 = TestHelper.getTestWebClient(baseExternalURI, "hello");
        RequestHeaders getHeaders2 = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

        // Issue request 1.
        HttpResponse response1 = webClient1.execute(getHeaders1);

        // Issue request 2.
        HttpResponse response2 = webClient2.execute(getHeaders2);

        // Complete 1.
        AggregatedHttpResponse aggregatedHttpResponse1 = response1.aggregate().join();
        ResponseHeaders headers1 = aggregatedHttpResponse1.headers();
        assertEquals("200", headers1.get(HttpHeaderNames.STATUS));

        // Complete 2.
        AggregatedHttpResponse aggregatedHttpResponse2 = response2.aggregate().join();
        ResponseHeaders headers2 = aggregatedHttpResponse2.headers();
        assertEquals("200", headers2.get(HttpHeaderNames.STATUS));

        assertEquals(4, FilibusterServer.payloadsReceived.size());

        // Verify invocation for first request.
        JSONObject webClient1InvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", webClient1InvocationPayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationWebClient1 = (DistributedExecutionIndex) startingExecutionIndex.clone();
        assertDistributedExecutionIndexInvocationWebClient1.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-5fc8161f610a6d83ed0e3d8abe8d8de935bb41ec-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationWebClient1.toString(), webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationWebClient2 = (DistributedExecutionIndex) startingExecutionIndex.clone();
        assertDistributedExecutionIndexInvocationWebClient2.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-8de910896d933b92106517d38b8da6f1bf29a5ec-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationWebClient2.toString(), webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationCompleteWebClient1 = (DistributedExecutionIndex) startingExecutionIndex.clone();
        assertDistributedExecutionIndexInvocationCompleteWebClient1.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-5fc8161f610a6d83ed0e3d8abe8d8de935bb41ec-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationCompleteWebClient1.toString(), webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        DistributedExecutionIndex assertDistributedExecutionIndexInvocationCompleteWebClient2 = (DistributedExecutionIndex) startingExecutionIndex.clone();
        assertDistributedExecutionIndexInvocationCompleteWebClient2.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-8de910896d933b92106517d38b8da6f1bf29a5ec-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
        assertEquals(assertDistributedExecutionIndexInvocationCompleteWebClient2.toString(), webClient2InvocationCompletePayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with headers, two futures, in loop.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsWithHeadersInLoop() {
        DistributedExecutionIndex startingDistributedExecutionIndex = createNewDistributedExecutionIndex();
        startingDistributedExecutionIndex.push("some-random-location-1337");

        setInitialVectorClock(null);
        setInitialOriginVectorClock(null);
        setInitialDistributedExecutionIndex(startingDistributedExecutionIndex.toString());
        setInitialRequestId("2");

        // Verify each iteration of the loop to ensure EI properly updates.
        for(int i = 0; i < 2; i++) {
            WebClient webClient3 = TestHelper.getTestWebClient(baseExternalURI, "hello");
            RequestHeaders getHeaders3 = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

            HttpResponse response3 = webClient3.execute(getHeaders3);

            AggregatedHttpResponse aggregatedHttpResponse3 = response3.aggregate().join();
            ResponseHeaders headers3 = aggregatedHttpResponse3.headers();
            assertEquals("200", headers3.get(HttpHeaderNames.STATUS));

            int invocationEntry = FilibusterServer.payloadsReceived.size() - 2;
            JSONObject webClient3InvocationPayload = FilibusterServer.payloadsReceived.get(invocationEntry);
            assertEquals("invocation", webClient3InvocationPayload.getString("instrumentation_type"));

            DistributedExecutionIndex assertDistributedExecutionIndexForWebClient3 = (DistributedExecutionIndex) startingDistributedExecutionIndex.clone();
            assertDistributedExecutionIndexForWebClient3.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0ffe16cbfc93643544ddf4f173cae84370155355-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
            if (i == 1) {
                assertDistributedExecutionIndexForWebClient3.pop();
                assertDistributedExecutionIndexForWebClient3.push("V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0ffe16cbfc93643544ddf4f173cae84370155355-0a33c850b8b1834c9e7ec64a7afa9982c6f092da");
            }
            assertEquals(assertDistributedExecutionIndexForWebClient3.toString(), webClient3InvocationPayload.getString("execution_index"));

            int invocationCompleteEntry = FilibusterServer.payloadsReceived.size() - 1;
            JSONObject webClient3InvocationCompletePayload = FilibusterServer.payloadsReceived.get(invocationCompleteEntry);
            assertEquals("invocation_complete", webClient3InvocationCompletePayload.getString("instrumentation_type"));
        }
    }
}
