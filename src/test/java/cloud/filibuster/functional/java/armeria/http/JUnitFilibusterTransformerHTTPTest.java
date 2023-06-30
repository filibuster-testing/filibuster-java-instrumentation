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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    private final List<String> validErrorCodes = Arrays.asList("404", "503");

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
                assertTrue(validErrorCodes.contains(statusCode));
            } else {
                assertEquals("200", statusCode);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(2, testErrorCodesReceived.size());
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @Test
    @Order(3)
    public void testNumberOfTestsExecuted() {
        assertEquals(6, numberOfTestsExecuted);
    }
}
