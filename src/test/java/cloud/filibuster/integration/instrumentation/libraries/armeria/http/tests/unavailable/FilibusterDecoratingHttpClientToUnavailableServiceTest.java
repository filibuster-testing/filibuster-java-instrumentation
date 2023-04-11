package cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.unavailable;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.integration.instrumentation.FilibusterServerFake;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.VectorClock;

import cloud.filibuster.integration.instrumentation.libraries.armeria.http.tests.FilibusterDecoratingHttpClientTest;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilibusterDecoratingHttpClientToUnavailableServiceTest extends FilibusterDecoratingHttpClientTest {
    @BeforeAll
    public static void startFilibusterServer() throws IOException, InterruptedException {
        startFilibuster();
    }

    @AfterAll
    public static void stopFilibusterServer() throws InterruptedException {
        stopFilibuster();
    }

    @Test
    @DisplayName("Test the Filibuster client decorator to unavailable service (with Filibuster unavailable.)")
    public void testClientDecoratorToUnavailableServiceWithFilibusterUnavailable() throws InterruptedException {
        String serviceName = "hello";

        assertThrows(CompletionException.class, () -> {
            WebClient webClient = TestHelper.getTestWebClient("http://localhost:65534", serviceName);
            RequestHeaders getHeaders = RequestHeaders.of(
                    HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
            webClient.execute(getHeaders).aggregate().join();
        });

        waitForWaitComplete();

        // Only the invocation and invocation_complete should be present.
        // No request_received because the destination was down.
        assertEquals(2, FilibusterServerFake.payloadsReceived.size());

        // There should only be a single key; verify the execution index.
        DistributedExecutionIndex distributedExecutionIndex = getFirstDistributedExecutionIndexFromMapping();
        assertNotNull(distributedExecutionIndex);
        assertEquals(getInitialDistributedExecutionIndex(), distributedExecutionIndex.toString());

        // Final clock should descend provided clock to account for incrementing for request.
        VectorClock finalVectorClock = getFirstVectorClockFromMapping();
        assertNotNull(finalVectorClock);
        assertTrue(VectorClock.descends(getInitialVectorClock(), finalVectorClock));
    }
}
