package cloud.filibuster.instrumentation.libraries.armeria.http.tests.available;

import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Property;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.instrumentation.libraries.armeria.http.tests.FilibusterDecoratingHttpClientTest;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilibusterDecoratingHttpClientToAvailableServiceTestInstrumentationDisabledByProperty extends FilibusterDecoratingHttpClientTest {
    private final String baseExternalURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";

    @BeforeEach
    public void startServices() throws IOException, InterruptedException {
        startFilibuster();
        startExternalServer();
    }

    @AfterEach
    public void stopServices() throws InterruptedException {
        stopFilibuster();
        stopExternalServer();
    }

    @AfterEach
    public void resetFilibusterConfiguration() {
        FilibusterServer.shouldReturnNotFounds = false;
        FilibusterServer.noNewTestExecution = false;
        FilibusterDecoratingHttpClient.disableServerCommunication = false;
    }

    @BeforeEach
    public void disableInstrumentationProperty() {
        Property.setInstrumentationEnabledProperty(false);
    }

    @AfterEach
    public void enableInstrumentationProperty() {
        Property.setInstrumentationEnabledProperty(true);
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to available service (with instrumentation disabled by property.)")
    public void testClientDecoratorToAvailableServiceWithInstrumentationDisabledByProperty() {
        String serviceName = "hello";

        WebClient webClient = TestHelper.getTestWebClient(baseExternalURI, serviceName);
        RequestHeaders getHeaders = RequestHeaders.of(
                HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();
        ResponseHeaders headers = response.headers();
        String statusCode = headers.get(HttpHeaderNames.STATUS);
        assertEquals("200", statusCode);

        // No assertions on payload -- no requests made.
        assertEquals(0, FilibusterServer.payloadsReceived.size());
    }
}
