package cloud.filibuster.functional;

import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;

import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.helpers.Property.setCallsiteLineNumberProperty;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearVectorClockForRequestId;

/**
 * Base call for JUnit tests that starts/stops the required services and resets the Filibuster configuration.
 */
public class JUnitBaseTest {
    @BeforeEach
    protected void startServices() throws IOException, InterruptedException {
        startHelloServerAndWaitUntilAvailable();
        startWorldServerAndWaitUntilAvailable();
        startExternalServerAndWaitUntilAvailable();
    }

    @AfterEach
    protected void stopServices() throws InterruptedException {
        stopExternalServerAndWaitUntilUnavailable();
        stopWorldServerAndWaitUntilUnavailable();
        stopHelloServerAndWaitUntilUnavailable();
    }

    @BeforeEach
    protected void resetMyHelloServiceState() {
        MyHelloService.shouldReturnRuntimeExceptionWithCause = false;
        MyHelloService.shouldReturnRuntimeExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithDescription = false;
        MyHelloService.shouldReturnExceptionWithCause = false;
    }

    @BeforeEach
    protected void resetFilibusterState() {
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();
    }

    @BeforeAll
    protected static void enablePrettyDistributedExecutionIndexes() {
        setCallsiteLineNumberProperty(false);
    }

    @AfterAll
    protected static void disablePrettyDistributedExecutionIndexes() {
        setCallsiteLineNumberProperty(true);
    }

    @BeforeEach
    protected void clearStateFromLastExecution() {
        clearDistributedExecutionIndexForRequestId();
        clearVectorClockForRequestId();
    }
}