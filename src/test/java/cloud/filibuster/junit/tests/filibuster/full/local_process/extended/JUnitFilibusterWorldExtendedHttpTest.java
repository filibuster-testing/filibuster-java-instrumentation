package cloud.filibuster.junit.tests.filibuster.full.local_process.extended;

import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.FilibusterTest;
import cloud.filibuster.junit.configuration.FilibusterWorldExtendedDefaultAnalysisConfigurationFile;
import cloud.filibuster.junit.interceptors.GitHubActionsSkipInvocationInterceptor;
import cloud.filibuster.junit.server.backends.FilibusterLocalProcessServerBackend;
import cloud.filibuster.junit.tests.filibuster.JUnitBaseTest;
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
import java.util.List;

import static cloud.filibuster.junit.Assertions.wasFaultInjected;
import static cloud.filibuster.junit.Assertions.wasFaultInjectedOnService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JUnitFilibusterWorldExtendedHttpTest extends JUnitBaseTest {
    private static int numberOfTestsExceptionsThrownFaultsInjected = 0;

    private final List<String> validErrorCodes = Arrays.asList("404", "503");

    /**
     * Inject faults between Hello and World using Filibuster and assert proper faults are injected.
     */
    @DisplayName("Test world route with Filibuster.")
    @FilibusterTest(analysisConfigurationFile=FilibusterWorldExtendedDefaultAnalysisConfigurationFile.class, serverBackend=FilibusterLocalProcessServerBackend.class)
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Order(1)
    public void testHelloAndWorldServiceWithFilibuster() {
        try {
            String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
            WebClient webClient = TestHelper.getTestWebClient(baseURI);
            RequestHeaders getHeaders = RequestHeaders.of(HttpMethod.GET, "/world", HttpHeaderNames.ACCEPT, "application/json");
            AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
            ResponseHeaders headers = response.headers();
            String statusCode = headers.get(HttpHeaderNames.STATUS);

            if (wasFaultInjected()) {
                numberOfTestsExceptionsThrownFaultsInjected++;
                assertTrue(validErrorCodes.contains(statusCode));
                assertTrue(wasFaultInjectedOnService("world"));
            } else {
                assertEquals("200", statusCode);
            }
        } catch (Throwable t) {
            assertFalse(true);
        }
    }

    /**
     * Verify that Filibuster generated the correct number of fault injections.
     */
    @DisplayName("Verify correct number of generated Filibuster tests.")
    @ExtendWith(GitHubActionsSkipInvocationInterceptor.class)
    @Test
    @Order(2)
    public void testNumAssertions() {
        assertEquals(5, numberOfTestsExceptionsThrownFaultsInjected);
    }
}
