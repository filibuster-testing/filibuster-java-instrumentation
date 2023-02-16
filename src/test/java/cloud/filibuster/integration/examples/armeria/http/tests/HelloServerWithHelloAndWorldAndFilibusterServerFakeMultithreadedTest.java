package cloud.filibuster.integration.examples.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.Callsite;
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

import static cloud.filibuster.integration.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.integration.examples.test_servers.HelloServer.setInitialDistributedExecutionIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloServerWithHelloAndWorldAndFilibusterServerFakeMultithreadedTest extends HelloServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        super.startHelloServer();
        super.startWorldServer();
        super.startExternalServer();
        super.startFilibuster();

        FilibusterServerFake.oneNewTestExecution = true;

        Callsite callsite = new Callsite("service", "class", "moduleName", new CallsiteArguments(Object.class, "deadbeef"));
        DistributedExecutionIndex startingDistributedExecutionIndex = createNewDistributedExecutionIndex();
        startingDistributedExecutionIndex.push(callsite);

        setInitialDistributedExecutionIndex(startingDistributedExecutionIndex.toString());
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        super.stopHelloServer();
        super.stopWorldServer();
        super.stopExternalServer();
        super.stopFilibuster();

        FilibusterServerFake.noNewTestExecution = false;

        resetInitialDistributedExecutionIndex();
    }

    @BeforeEach
    public void enableFilibuster() {
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
        FilibusterDecoratingHttpService.disableInstrumentation = false;
    }

    @Test
    @DisplayName("Test hello server multithreaded route (with Filibuster.)")
    public void testMultithreadedWithFilibuster() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/multithreaded", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Very proper number of Filibuster records.
        assertEquals(4, FilibusterServerFake.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-1532c7f1ead5eb0c4a447b9589c22763fb129cbf-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", firstInvocationPayload.getString("execution_index"));

        JSONObject firstInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-1532c7f1ead5eb0c4a447b9589c22763fb129cbf-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a0bdda920880c4f6377eea891cfacccf183d6b66-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", secondInvocationPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a0bdda920880c4f6377eea891cfacccf183d6b66-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", secondInvocationCompletedPayload.getString("execution_index"));
    }

    @Test
    @DisplayName("Test hello server multithreaded route (with Filibuster, same EI.)")
    public void testMultithreadedWithFilibusterSameEI() {
        // Get remote resource.
        String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI);
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/multithreaded-with-same-ei", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

        // Get headers and verify a 200 response.
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // Very proper number of Filibuster records.
        assertEquals(4, FilibusterServerFake.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-b282b2b9281c7895c18e70e9a85f02e1acde5c15-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", firstInvocationPayload.getString("execution_index"));

        JSONObject firstInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-b282b2b9281c7895c18e70e9a85f02e1acde5c15-0a8127ad87a9ab6630533750add635c52d003488\", 1]]", firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-b282b2b9281c7895c18e70e9a85f02e1acde5c15-0a8127ad87a9ab6630533750add635c52d003488\", 2]]", secondInvocationPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-7c16a1567a8511780fc167514344025c9dbdf3c0-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-b282b2b9281c7895c18e70e9a85f02e1acde5c15-0a8127ad87a9ab6630533750add635c52d003488\", 2]]", secondInvocationCompletedPayload.getString("execution_index"));
    }
}
