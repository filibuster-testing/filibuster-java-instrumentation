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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.Random;

import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTransformerHTTPTest {
    private static int numberOfTestsExecuted = 0;

    @BeforeAll
    public static void startHelloService() throws IOException, InterruptedException {
        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
    }

    @DisplayName("Test injecting 'null' as HTTP response using transformers.")
    @TestWithFilibuster(analysisConfigurationFile = FilibusterHTTPNullTransformerAnalysisConfigurationFile.class)
    @Order(1)
    public void testInjectNullInHTTP() {
        numberOfTestsExecuted++;
        String cookie = String.valueOf(new Random(0).nextInt());

        try {
            String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
            WebClient webClient = TestHelper.getTestWebClient(baseURI, "test");
            RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/echo-cookie", HttpHeaderNames.ACCEPT, "application/json");
            getHeaders = getHeaders.toBuilder().add("cookie", cookie).build();
            AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
            ResponseHeaders headers = response.headers();
            String statusCode = headers.get(HttpHeaderNames.STATUS);

            assertEquals("200", statusCode);

            if (wasFaultInjected()) {
                assertEquals("null", response.content().toStringAscii());
            } else {
                assertEquals(cookie, response.content().toStringAscii());
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    @DisplayName("Verify correct number of test execution.")
    @Test
    @Order(2)
    public void testNumExecutions() {
        // 1 for the reference execution and 1 for the test with the injected transformer fault
        assertEquals(2, numberOfTestsExecuted);
    }

}
