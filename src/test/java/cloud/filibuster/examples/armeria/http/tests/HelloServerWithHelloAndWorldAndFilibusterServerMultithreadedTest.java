package cloud.filibuster.examples.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
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

import static cloud.filibuster.examples.test_servers.HelloServer.resetInitialDistributedExecutionIndex;
import static cloud.filibuster.examples.test_servers.HelloServer.setInitialDistributedExecutionIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloServerWithHelloAndWorldAndFilibusterServerMultithreadedTest extends HelloServerTest {
    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        super.startHelloServer();
        super.startWorldServer();
        super.startExternalServer();
        super.startFilibuster();

        FilibusterServer.oneNewTestExecution = true;

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

        FilibusterServer.noNewTestExecution = false;

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
        assertEquals(4, FilibusterServer.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-5debe6073ffa4f016a7764fe919145bd85031cf4-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", firstInvocationPayload.getString("execution_index"));

        JSONObject firstInvocationCompletedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-5debe6073ffa4f016a7764fe919145bd85031cf4-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-30564c557e80fb964390daf4507d803a360f3ae3-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", secondInvocationPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-30564c557e80fb964390daf4507d803a360f3ae3-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", secondInvocationCompletedPayload.getString("execution_index"));
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
        assertEquals(4, FilibusterServer.payloadsReceived.size());

        // These are the required Filibuster instrumentation fields for this call.

        JSONObject firstInvocationPayload = FilibusterServer.payloadsReceived.get(0);
        assertEquals("invocation", firstInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-feaf26703eccaeb393f2adbc2778988bc8b80c1b-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", firstInvocationPayload.getString("execution_index"));

        JSONObject firstInvocationCompletedPayload = FilibusterServer.payloadsReceived.get(1);
        assertEquals("invocation_complete", firstInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-feaf26703eccaeb393f2adbc2778988bc8b80c1b-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 1]]", firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServer.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-feaf26703eccaeb393f2adbc2778988bc8b80c1b-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 2]]", secondInvocationPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServer.payloadsReceived.get(3);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-3f36509a95d2905e9c5f0d4d69abf4e53328e53c-f49cf6381e322b147053b74e4500af8533ac1e4c\", 1], [\"V1-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-feaf26703eccaeb393f2adbc2778988bc8b80c1b-0a33c850b8b1834c9e7ec64a7afa9982c6f092da\", 2]]", secondInvocationCompletedPayload.getString("execution_index"));
    }
}
