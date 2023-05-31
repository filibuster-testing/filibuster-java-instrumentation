package cloud.filibuster.functional.python.basic;

import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.TestWithFilibuster;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import cloud.filibuster.junit.server.backends.FilibusterLocalProcessServerBackend;
import cloud.filibuster.functional.JUnitBaseTest;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterHttpTest extends JUnitBaseTest {
    private final static Set<String> testErrorCodesReceived = new HashSet<>();

    private static int numberOfTestsExecuted = 0;

    private final List<String> validErrorCodes = Arrays.asList("404", "503");

    /**
     * Inject faults between Hello and World using Filibuster and assert proper faults are injected.
     */
    @DisplayName("Test world route with Filibuster.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @TestWithFilibuster(serverBackend=FilibusterLocalProcessServerBackend.class)
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
                assertTrue(wasFaultInjectedOnService("world"));
            } else {
                assertEquals("200", statusCode);
            }
        } catch (Throwable t) {
            assertFalse(true);
        }
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(2, testErrorCodesReceived.size());
    }

    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(3)
    public void testNumberOfTestsExecuted() {
        assertEquals(5, numberOfTestsExecuted);
    }
}
