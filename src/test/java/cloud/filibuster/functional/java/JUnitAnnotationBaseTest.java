package cloud.filibuster.functional.java;

import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static cloud.filibuster.instrumentation.helpers.Property.setCallsiteLineNumberProperty;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearVectorClockForRequestId;
import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;

public class JUnitAnnotationBaseTest {

    @BeforeAll
    public static void setupTest() throws IOException, InterruptedException {
        // Start services.
        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
        startExternalServerAndWaitUntilAvailable();

        // Reset service configuration.
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;

        // Reset Filibuster state.
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();

        // Set up Filibuster configuration.
        setCallsiteLineNumberProperty(false);

        // Clear any accumulated state.
        clearDistributedExecutionIndexForRequestId();
        clearVectorClockForRequestId();
    }

    @AfterAll
    public static void teardownTest() throws InterruptedException {
        // Stop services.
        stopExternalServerAndWaitUntilUnavailable();
        stopWorldServerAndWaitUntilUnavailable();
        stopHelloServerAndWaitUntilUnavailable();

        // Restore Filibuster configuration.
        setCallsiteLineNumberProperty(true);
    }
}