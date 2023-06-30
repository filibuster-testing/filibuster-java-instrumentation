package cloud.filibuster.functional.java.armeria.http;

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
import java.util.HashSet;
import java.util.Set;

import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterTransformerHTTPTest {
    private final static Set<String> testErrorCodesReceived = new HashSet<>();

    private static int numberOfTestsExecuted = 0;

    @BeforeAll
    public static void startHelloService() throws IOException, InterruptedException {
        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
    }

    @DisplayName("Test world route with Filibuster.")
    @TestWithFilibuster(analysisConfigurationFile = FilibusterHTTPNullTransformerAnalysisConfigurationFile.class)
    @Order(1)
    public void testHelloAndWorldServiceWithFilibuster() {
        numberOfTestsExecuted++;

        try {
            String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
            WebClient webClient = TestHelper.getTestWebClient(baseURI);
            RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world", HttpHeaderNames.ACCEPT, "application/json");
            AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
            ResponseHeaders headers = response.headers();
            String statusCode = headers.get(HttpHeaderNames.STATUS);

            if (wasFaultInjected()) {
                testErrorCodesReceived.add(statusCode);
            } else {
                assertEquals("200", statusCode);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    @DisplayName("Verify correct number of test execution.")
    @Test
    @Order(2)
    public void testNumberOfTestsExecuted() {
        // 1 for the reference execution and 1 for the test with the injected transformer fault
        assertEquals(2, numberOfTestsExecuted);
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumAssertions() {
        // 1 fault for the transformer value 'null'
        assertEquals(1, testErrorCodesReceived.size());

        // Injecting the transformer value 'null' leads to an error code of '503'
        assertTrue(testErrorCodesReceived.contains("503"));
    }
}
