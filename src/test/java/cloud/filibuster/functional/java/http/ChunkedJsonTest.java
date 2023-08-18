package cloud.filibuster.functional.java.http;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.configuration.examples.armeria.http.FilibusterHTTPNullTransformerAnalysisConfigurationFile;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.Random;

import static cloud.filibuster.integration.instrumentation.TestHelper.startAPIServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopAPIServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static cloud.filibuster.junit.assertions.protocols.GenericAssertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChunkedJsonTest {
    @BeforeAll
    public static void startAllServices() throws IOException, InterruptedException {
        startAPIServerAndWaitUntilAvailable();
    }

    @AfterAll
    public static void stopAllServices() throws InterruptedException {
        stopAPIServerAndWaitUntilUnavailable();
    }

    @TestWithFilibuster(maxIterations = 1)
    @Order(1)
    public void testChunkedJson() {
        String baseURI = "http://" + Networking.getHost("api_server") + ":" + Networking.getPort("api_server") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI, "test");
        RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/chunked-json", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);
    }
}
