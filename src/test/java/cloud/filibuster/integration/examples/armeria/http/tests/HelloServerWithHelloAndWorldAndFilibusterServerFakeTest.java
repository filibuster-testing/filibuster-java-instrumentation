package cloud.filibuster.integration.examples.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;

import cloud.filibuster.integration.examples.test_servers.HelloServer;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
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

public class HelloServerWithHelloAndWorldAndFilibusterServerFakeTest extends HelloServerTest {
    @BeforeAll
    public static void startServices() throws IOException, InterruptedException {
        startHelloServer();
        startWorldServer();
        startExternalServer();
        startFilibuster();
    }

    @BeforeEach
    public void resetConfigurationBeforeEach() {
        FilibusterServerFake.oneNewTestExecution = true;
        FilibusterServerFake.resetPayloadsReceived();

        Callsite callsite = new Callsite("service", "class", "moduleName", new CallsiteArguments(Object.class, "deadbeef"));
        DistributedExecutionIndex startingDistributedExecutionIndex = createNewDistributedExecutionIndex();
        startingDistributedExecutionIndex.push(callsite);

        HelloServer.setInitialDistributedExecutionIndex(startingDistributedExecutionIndex.toString());
    }

    @AfterAll
    public static void stopServices() throws InterruptedException {
        stopHelloServer();
        stopWorldServer();
        stopExternalServer();
        stopFilibuster();
    }

    @AfterEach
    public void resetConfigurationAfterEach() {
        FilibusterServerFake.noNewTestExecution = false;

        HelloServer.resetInitialDistributedExecutionIndex();
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-d3a2055abe493634f8a55af76b77389b44e6d50b-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", invocationCompletedPayload.getString("execution_index"));
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-6250b3336c79be82b1075d890bf944ed7a39f2d6-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", firstInvocationCompletedPayload.getString("execution_index"));

        JSONObject secondInvocationCompletedPayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("invocation_complete", secondInvocationCompletedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-f4282cd12ba68c7807a5ae02866ef3f806a3d96c-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", secondInvocationCompletedPayload.getString("execution_index"));
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3c9829748a043c9ac189483f74cf9dd389ecf462-9fe227e4fead63172f58e4dda36cf507802a3d54\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3c9829748a043c9ac189483f74cf9dd389ecf462-9fe227e4fead63172f58e4dda36cf507802a3d54\", 1]]", firstRequestReceivedPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3c9829748a043c9ac189483f74cf9dd389ecf462-9fe227e4fead63172f58e4dda36cf507802a3d54\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-3468b1cf3b611efd5f8287713d408d2418d0a720-31ec222c12070c86e1058ef0709e801dee10e5eb\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3c9829748a043c9ac189483f74cf9dd389ecf462-9fe227e4fead63172f58e4dda36cf507802a3d54\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-3468b1cf3b611efd5f8287713d408d2418d0a720-31ec222c12070c86e1058ef0709e801dee10e5eb\", 1]]", secondRequestReceivedPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3c9829748a043c9ac189483f74cf9dd389ecf462-9fe227e4fead63172f58e4dda36cf507802a3d54\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-3468b1cf3b611efd5f8287713d408d2418d0a720-31ec222c12070c86e1058ef0709e801dee10e5eb\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-3c9829748a043c9ac189483f74cf9dd389ecf462-9fe227e4fead63172f58e4dda36cf507802a3d54\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1]]", secondRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationPayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation", thirdInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-af2d14276decd1037b1078f4948f7180d44b14cb-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", thirdInvocationPayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationPayload.getJSONObject("vclock").toString());

        JSONObject thirdRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("request_received", thirdRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-af2d14276decd1037b1078f4948f7180d44b14cb-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", thirdRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(6);
        assertEquals("invocation_complete", thirdInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-af2d14276decd1037b1078f4948f7180d44b14cb-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", thirdInvocationCompletePayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(7);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(8);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-aac3882547f0da3b3fab2915baf8ebe5cc99f3e8-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1]]", firstInvocationPayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationPayload.getJSONObject("vclock").toString());

        JSONObject firstRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("request_received", firstRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1]]", firstRequestReceivedPayload.getString("execution_index"));

        JSONObject secondInvocationPayload = FilibusterServerFake.payloadsReceived.get(2);
        assertEquals("invocation", secondInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1]]", secondInvocationPayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationPayload.getJSONObject("vclock").toString());

        JSONObject secondRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(3);
        assertEquals("request_received", secondRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1]]", secondRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationPayload = FilibusterServerFake.payloadsReceived.get(4);
        assertEquals("invocation", thirdInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-af2d14276decd1037b1078f4948f7180d44b14cb-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", thirdInvocationPayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationPayload.getJSONObject("vclock").toString());

        JSONObject thirdRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(5);
        assertEquals("request_received", thirdRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-af2d14276decd1037b1078f4948f7180d44b14cb-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", thirdRequestReceivedPayload.getString("execution_index"));

        JSONObject thirdInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(6);
        assertEquals("invocation_complete", thirdInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-af2d14276decd1037b1078f4948f7180d44b14cb-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", thirdInvocationCompletePayload.getString("execution_index"));
        assertEquals(thirdRequestVectorClock.toJSONObject().toString(), thirdInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject secondInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(7);
        assertEquals("invocation_complete", secondInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-7c211433f02071597741e6ff5a8ea34789abbf43-bf801c417a24769c151e3729f35ee3e62e4e04d4-f8491880ebbad205fecdf806b09c197d9a3f78f3-e800ba475f7dfd7a2bce93a089d23cf8042e91ce\", 1]]", secondInvocationCompletePayload.getString("execution_index"));
        assertEquals(secondRequestVectorClock.toJSONObject().toString(), secondInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject firstInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(8);
        assertEquals("invocation_complete", firstInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-e3d8e835e8e707c43880b32c534ce5803f92fcb9-b45743a354afc18ff2ce47ff02b4a995f74b8a7e\", 1]]", firstInvocationCompletePayload.getString("execution_index"));
        assertEquals(firstRequestVectorClock.toJSONObject().toString(), firstInvocationCompletePayload.getJSONObject("vclock").toString());

        JSONObject fourthInvocationPayload = FilibusterServerFake.payloadsReceived.get(9);
        assertEquals("invocation", fourthInvocationPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-217de8f611b2e6faf8c40f7ab14f6af54a6279c9-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", fourthInvocationPayload.getString("execution_index"));
        assertEquals(fourthRequestVectorClock.toJSONObject().toString(), fourthInvocationPayload.getJSONObject("vclock").toString());

        JSONObject fourthRequestReceivedPayload = FilibusterServerFake.payloadsReceived.get(10);
        assertEquals("request_received", fourthRequestReceivedPayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-217de8f611b2e6faf8c40f7ab14f6af54a6279c9-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", fourthRequestReceivedPayload.getString("execution_index"));

        JSONObject fourthInvocationCompletePayload = FilibusterServerFake.payloadsReceived.get(11);
        assertEquals("invocation_complete", fourthInvocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-bf801c417a24769c151e3729f35ee3e62e4e04d4-217de8f611b2e6faf8c40f7ab14f6af54a6279c9-18144e053c40e4eef0d385c9803ae1a95a8c48a2\", 1]]", fourthInvocationCompletePayload.getString("execution_index"));
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-102f766bb1dc5da807c4d20fbc6d9bfdd8347df2-606e5fae9ca65e1dc4a2eaa01965a38fdc440b3a-5a0744effc54f948069850d40d4d162d17a5ca7a\", 1]]", invocationPayload.getString("execution_index"));

        JSONObject invocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", invocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-102f766bb1dc5da807c4d20fbc6d9bfdd8347df2-606e5fae9ca65e1dc4a2eaa01965a38fdc440b3a-5a0744effc54f948069850d40d4d162d17a5ca7a\", 1]]", invocationCompletePayload.getString("execution_index"));
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
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-602a8b352203c37ab065a38dcef581db609159ff-c4b7974c575d9adb92c89f9ca702238b52a4145b-7711c79dc75963a318976577a802fb2950eef516\", 1]]", invocationPayload.getString("execution_index"));

        JSONObject invocationCompletePayload = FilibusterServerFake.payloadsReceived.get(1);
        assertEquals("invocation_complete", invocationCompletePayload.getString("instrumentation_type"));
        assertEquals("[[\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-4cf5bc59bee9e1c44c6254b5f84e7f066bd8e5fe-a468b76d6940d5e59a854b8c01bb25e7e202be04-37194a99ac7ae02bdd3c05f13e103e37235f2b33-00aa7adca5809bf3003b7469bdf22140ac380041\", 1], [\"V1-da39a3ee5e6b4b0d3255bfef95601890afd80709-aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d-602a8b352203c37ab065a38dcef581db609159ff-c4b7974c575d9adb92c89f9ca702238b52a4145b-7711c79dc75963a318976577a802fb2950eef516\", 1]]", invocationCompletePayload.getString("execution_index"));
    }
}


