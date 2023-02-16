package cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.available;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
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
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-dc15453486b129e22813862a6ace66a6df28fd97-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-9276a3307475a4d038bb3e66a1fb5ddc450cc8bd-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-dc15453486b129e22813862a6ace66a6df28fd97-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-9276a3307475a4d038bb3e66a1fb5ddc450cc8bd-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient2InvocationCompletePayload.getString("execution_index"));

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

            assertEquals("[[\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-87fd6682e124f07860e3b0f170c05754092723e1-0a8127ad87a9ab6630533750add635c52d003488\", " + (i + 1) + "]]", webClient3InvocationPayload.getString("execution_index"));

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
        Callsite callsite = new Callsite("service", "class", "moduleName", new CallsiteArguments(Object.class, "deadbeef"));
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
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c4ce57070c07d32d6b0baf7ed5af7b77c9dd0fb2-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-c559b7d44f8e12fdf54a75b4bbd073d1589ddb23-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c4ce57070c07d32d6b0baf7ed5af7b77c9dd0fb2-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-609d6834f8ac7da3e3871732999b4082016e48fc-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c4ce57070c07d32d6b0baf7ed5af7b77c9dd0fb2-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-c559b7d44f8e12fdf54a75b4bbd073d1589ddb23-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-c4ce57070c07d32d6b0baf7ed5af7b77c9dd0fb2-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-609d6834f8ac7da3e3871732999b4082016e48fc-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", webClient2InvocationCompletePayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with headers, two futures, in loop.)")
    public void testClientDecoratorToAvailableServiceTwoRequestsWithHeadersInLoop() {
        Callsite callsite = new Callsite("service", "class", "moduleName", new CallsiteArguments(Object.class, "deadbeef"));
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

            assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-be25f601f3dd3fb0453e5c501bbc9430d554952b-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-60c93b89d0ccacddbfe11d1b3f5729f5fe4e68ec-0a8127ad87a9ab6630533750add635c52d003488\", " + (i + 1) + "]]", webClient3InvocationPayload.getString("execution_index"));

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
