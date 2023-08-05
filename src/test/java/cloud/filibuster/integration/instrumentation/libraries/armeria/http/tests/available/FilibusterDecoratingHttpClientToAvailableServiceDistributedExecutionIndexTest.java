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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-25b149611e32f77f281ab2b54611e6f9c2fb4592-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-d8c0b3db3be8f07e8a744ace159fa1db1348c455-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-25b149611e32f77f281ab2b54611e6f9c2fb4592-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-d8c0b3db3be8f07e8a744ace159fa1db1348c455-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient2InvocationCompletePayload.getString("execution_index"));

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

            assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0b0997231ec03b1e7536e1f1f0c4e6e5fe3095e6-efcac005fd5764253ef51e64f89f80aa3694defe\", " + (i + 1) + "]]", webClient3InvocationPayload.getString("execution_index"));

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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-2a4b8fe016f0bccd9424560a8ee084c9f4c05d88-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-15199a3902a1f6301d18d7298489fa1cc28316f9-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient1InvocationPayload.getString("execution_index"));

        // Verify invocation for second request.
        JSONObject webClient2InvocationPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation", webClient2InvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-2a4b8fe016f0bccd9424560a8ee084c9f4c05d88-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-9ee0f0c0689595e7356785bbfe1b47345f29e63b-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient2InvocationPayload.getString("execution_index"));

        // Verify invocation_complete for first request.
        JSONObject webClient1InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", webClient1InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-2a4b8fe016f0bccd9424560a8ee084c9f4c05d88-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-15199a3902a1f6301d18d7298489fa1cc28316f9-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient1InvocationCompletePayload.getString("execution_index"));

        // Verify invocation_complete for second request.
        JSONObject webClient2InvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", webClient2InvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-2a4b8fe016f0bccd9424560a8ee084c9f4c05d88-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-9ee0f0c0689595e7356785bbfe1b47345f29e63b-efcac005fd5764253ef51e64f89f80aa3694defe\", 1]]", webClient2InvocationCompletePayload.getString("execution_index"));
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

            assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-612fff0d2d96458fe5de5416ad5a32162db3c889-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-17bea544b5d35436384e61015f4f905377c0891c-efcac005fd5764253ef51e64f89f80aa3694defe\", " + (i + 1) + "]]", webClient3InvocationPayload.getString("execution_index"));

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
