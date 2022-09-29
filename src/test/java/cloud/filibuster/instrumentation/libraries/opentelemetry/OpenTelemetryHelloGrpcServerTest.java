package cloud.filibuster.instrumentation.libraries.opentelemetry;

import cloud.filibuster.instrumentation.FilibusterBaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static cloud.filibuster.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.startMockFilibusterServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.helpers.Property.setCallsiteHashCallsiteProperty;
import static cloud.filibuster.instrumentation.helpers.Property.setCallsiteIncludeStackTraceProperty;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OpenTelemetryHelloGrpcServerTest extends FilibusterBaseTest {
    @BeforeAll
    public static void enablePrettyDistributedExecutionIndexes() {
        setCallsiteHashCallsiteProperty(false);
        setCallsiteIncludeStackTraceProperty(false);
    }

    @AfterAll
    public static void disablePrettyDistributedExecutionIndexes() {
        setCallsiteHashCallsiteProperty(true);
        setCallsiteIncludeStackTraceProperty(true);
    }

    public void startHello() throws InterruptedException, IOException {
        startHelloServerAndWaitUntilAvailable();
    }

    public void stopHello() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
    }

    public void startWorld() throws InterruptedException, IOException {
        startWorldServerAndWaitUntilAvailable();
    }

    public void stopWorld() throws InterruptedException {
        stopWorldServerAndWaitUntilUnavailable();
    }

    public void startFilibuster() throws InterruptedException, IOException {
        startMockFilibusterServerAndWaitUntilAvailable();
    }

    public void stopFilibuster() throws InterruptedException {
        stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    public void startExternalServer() throws InterruptedException, IOException {
        startExternalServerAndWaitUntilAvailable();
    }

    public void stopExternalServer() throws InterruptedException {
        stopExternalServerAndWaitUntilUnavailable();
    }
}