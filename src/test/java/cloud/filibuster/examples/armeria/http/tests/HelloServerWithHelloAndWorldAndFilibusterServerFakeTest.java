package cloud.filibuster.examples.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.FilibusterServerFake;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.Callsite;
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

public class HelloServerWithHelloAndWorldAndFilibusterServerFakeTest extends HelloServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        super.startHelloServer();
        super.startWorldServer();
        super.startExternalServer();
        super.startFilibuster();

        FilibusterServerFake.oneNewTestExecution = true;

        Callsite callsite = new Callsite("service", "class", "moduleName", "deadbeef");
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

        // Very proper number of Filibuster records.
        assertEquals(3, FilibusterServerFake.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.
        JSONObject invocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", invocationCompletedPayload.getString("instrumentation_type"));
        assertEquals(0, invocationCompletedPayload.getInt("generated_id"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-6cd3d6dd444ad8b8f09a8be9fc703fda4e9fd688-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", invocationCompletedPayload.getString("execution_index"));
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
        assertEquals(6, FilibusterServerFake.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation_complete", firstInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0b59d3a81ebc0d76ab63bd0ad4019e866b1e94f4-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-ae15e4507b2517c2a485e52edca729408afddf0b-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", secondInvocationCompletedPayload.getString("execution_index"));
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
        assertEquals(6, FilibusterServerFake.payloadsReceived.size());

        // Assemble vector clocks.

        VectorClock firstRequestVectorClock = new VectorClock();
        firstRequestVectorClock.incrementClock("hello");

        VectorClock secondRequestVectorClock = new VectorClock();
        secondRequestVectorClock.incrementClock("hello");
        secondRequestVectorClock.incrementClock("world");

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-491375cc660dd6bbc392a9657e2b283a45bd970b-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-491375cc660dd6bbc392a9657e2b283a45bd970b-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1\", 1]]", firstRequestReceivedPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-491375cc660dd6bbc392a9657e2b283a45bd970b-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-c36469a105518a038019d230d75b2e575a7e2604-f05ca0f49ff94c87bb10dd4265940b0bba02dcb2\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-491375cc660dd6bbc392a9657e2b283a45bd970b-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-c36469a105518a038019d230d75b2e575a7e2604-f05ca0f49ff94c87bb10dd4265940b0bba02dcb2\", 1]]", secondRequestReceivedPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-491375cc660dd6bbc392a9657e2b283a45bd970b-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-c36469a105518a038019d230d75b2e575a7e2604-f05ca0f49ff94c87bb10dd4265940b0bba02dcb2\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-491375cc660dd6bbc392a9657e2b283a45bd970b-3faf16c421edacc1b4954b48cfd5b5e08d77f3f1\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
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
        assertEquals(9, FilibusterServerFake.payloadsReceived.size());

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

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1]]", secondRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationPayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation", thirdInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a77d0d148177ecc7941b6661d37f92bcd5caf1c8-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", thirdInvocationPayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationPayload.getJSONObject("vclock").toString());

        JSONObject thirdRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("request_received", thirdRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a77d0d148177ecc7941b6661d37f92bcd5caf1c8-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", thirdRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(6);
        assertEquals("invocation_complete", thirdInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a77d0d148177ecc7941b6661d37f92bcd5caf1c8-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", thirdInvocationCompletePayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(7);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(8);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3e1eae960024cc84709724e1bf4299c042bdc6f9-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
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
        assertEquals(12, FilibusterServerFake.payloadsReceived.size());

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

        JSONObject firstInvocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1]]", secondRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationPayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation", thirdInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a77d0d148177ecc7941b6661d37f92bcd5caf1c8-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", thirdInvocationPayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationPayload.getJSONObject("vclock").toString());

        JSONObject thirdRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("request_received", thirdRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a77d0d148177ecc7941b6661d37f92bcd5caf1c8-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", thirdRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(6);
        assertEquals("invocation_complete", thirdInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-a77d0d148177ecc7941b6661d37f92bcd5caf1c8-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", thirdInvocationCompletePayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(7);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1], [\"V1-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-7e7340bf6a4a6ae52d91786740892a93abd44b0c-44f8aba85b6dc43f841d4ae2648ad1c7a922a331\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(8);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-0d9efcd52a67859dc086937d6f82beca0777c629-cb508e66d0f8ee1ef4546567356eb01c51cb9598\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject fourthInvocationPayload = FilibusterServerFake.payloadsReceived.get(9);
        assertEquals("invocation", fourthInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f4bf271964860e1d1b08fa5420259a48862c4e1c-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", fourthInvocationPayload.getString("execution_index"));
        assertEquals(fourthRequestVectorClock.toJSONObject().toString(), fourthInvocationPayload.getJSONObject("vclock").toString());

        JSONObject fourthRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(10);
        assertEquals("request_received", fourthRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f4bf271964860e1d1b08fa5420259a48862c4e1c-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", fourthRequestReceivedPayload.getString("execution_index"));

        JSONObject fourthInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(11);
        assertEquals("invocation_complete", fourthInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f4bf271964860e1d1b08fa5420259a48862c4e1c-b4604597df48b0eae13cf0c110562e150ace79ff\", 1]]", fourthInvocationCompletePayload.getString("execution_index"));
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

        // Very proper number of Filibuster records.
        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject invocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", invocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-102f766bb1dc5da807c4d20fbc6d9bfdd8347df2-12b758730115cd0124b4ad66a8664c60e485170f-2d1ef8410983c2242adbaa0457f03575b8c06b92\", 1]]", invocationPayload.getString("execution_index"));

        JSONObject invocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", invocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-102f766bb1dc5da807c4d20fbc6d9bfdd8347df2-12b758730115cd0124b4ad66a8664c60e485170f-2d1ef8410983c2242adbaa0457f03575b8c06b92\", 1]]", invocationCompletePayload.getString("execution_index"));
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

        // Very proper number of Filibuster records.
        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject invocationPayload = FilibusterServerFake.payloadsReceived.get(0);
        assertEquals("invocation", invocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-602a8b352203c37ab065a38dcef581db609159ff-3e9e6feaf07535b8aa2fd327f5f65231b3a1210b-a547a5665dc2aca5823133e2152f4333fb9a36cc\", 1]]", invocationPayload.getString("execution_index"));

        JSONObject invocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", invocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-ab63ed66c502f15f3e2d7c1c67635a11a716146d-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-602a8b352203c37ab065a38dcef581db609159ff-3e9e6feaf07535b8aa2fd327f5f65231b3a1210b-a547a5665dc2aca5823133e2152f4333fb9a36cc\", 1]]", invocationCompletePayload.getString("execution_index"));
    }
}


