package cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.available;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;

import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.FilibusterDecoratingHttpClientTest;
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
        FilibusterServerFake.noNewTestExecution = true;
    }

    @AfterEach
    public void newTestExecution() {
        FilibusterServerFake.noNewTestExecution = false;
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

        assertEquals(4, FilibusterServerFake.payloadsReceived.size());

        // Verify invocation for first request.
        JSONObject webClient1InvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", webClient1InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-25b149611e32f77f281ab2b54611e6f9c2fb4592-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-25b149611e32f77f281ab2b54611e6f9c2fb4592-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-25b149611e32f77f281ab2b54611e6f9c2fb4592-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-25b149611e32f77f281ab2b54611e6f9c2fb4592-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient2InvocationCompletePayload.getString("execution_index"));

        resetInitialRequestId();
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

            int invocationEntry = FilibusterServerFake.payloadsReceived.size() - 2;
            JSONObject webClient3InvocationPayload = FilibusterServerFake.payloadsReceived.get(invocationEntry);
            assertEquals("invocation", webClient3InvocationPayload.getString("instrumentation_type"));

            assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-428016117ff93ef55ef110c32921f146f89436d1-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", " + (i + 1) + "]]", webClient3InvocationPayload.getString("execution_index"));

            int invocationCompleteEntry = FilibusterServerFake.payloadsReceived.size() - 1;
            JSONObject webClient3InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(invocationCompleteEntry);
            assertEquals("invocation_complete", webClient3InvocationCompletePayload.getString("instrumentation_type"));
        }

        resetInitialRequestId();
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with headers, two futures.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsWithHeaders() {
        DistributedExecutionIndex startingExecutionIndex = createNewDistributedExecutionIndex();
        Callsite callsite = new Callsite("service", "class", "moduleName", "deadbeef");
        startingExecutionIndex.push(callsite);

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

        assertEquals(4, FilibusterServerFake.payloadsReceived.size());

        // Verify invocation for first request.
        JSONObject webClient1InvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", webClient1InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c921992efc0b8901cc63920851c46e2ff03f5e40-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0b0a3129c232d8c98fd9261b1ef52aa377e14a73-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c921992efc0b8901cc63920851c46e2ff03f5e40-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-fe440ce5baf841b18c2df4cb277b4d11dd5bbe15-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c921992efc0b8901cc63920851c46e2ff03f5e40-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0b0a3129c232d8c98fd9261b1ef52aa377e14a73-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c921992efc0b8901cc63920851c46e2ff03f5e40-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-fe440ce5baf841b18c2df4cb277b4d11dd5bbe15-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", webClient2InvocationCompletePayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with headers, two futures, in loop.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsWithHeadersInLoop() {
        Callsite callsite = new Callsite("service", "class", "moduleName", "deadbeef");
        DistributedExecutionIndex startingDistributedExecutionIndex = createNewDistributedExecutionIndex();
        startingDistributedExecutionIndex.push(callsite);

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

            int invocationEntry = FilibusterServerFake.payloadsReceived.size() - 2;
            JSONObject webClient3InvocationPayload = FilibusterServerFake.payloadsReceived.get(invocationEntry);
            assertEquals("invocation", webClient3InvocationPayload.getString("instrumentation_type"));

            assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c6a41c91e787568b8f0c269ac93ddcb2ae1c8b35-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-46f9386325a6ca5ab5a825af7fec20625c414e0f-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", " + (i + 1) + "]]", webClient3InvocationPayload.getString("execution_index"));

            int invocationCompleteEntry = FilibusterServerFake.payloadsReceived.size() - 1;
            JSONObject webClient3InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(invocationCompleteEntry);
            assertEquals("invocation_complete", webClient3InvocationCompletePayload.getString("instrumentation_type"));
        }

        resetInitialVectorClock();
        resetInitialOriginVectorClock();
        resetInitialDistributedExecutionIndex();
        resetInitialRequestId();
    }
}
