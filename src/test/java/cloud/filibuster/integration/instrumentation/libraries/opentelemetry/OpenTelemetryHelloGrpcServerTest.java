package cloud.filibuster.integration.instrumentation.libraries.opentelemetry;

import cloud.filibuster.integration.instrumentation.FilibusterBaseTest;
import cloud.filibuster.integration.instrumentation.TestHelper;

import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OpenTelemetryHelloGrpcServerTest extends FilibusterBaseTest {
    public void startHello() throws InterruptedException, IOException {
        TestHelper.startHelloServerAndWaitUntilAvailable();
    }

    public void stopHello() throws InterruptedException {
        TestHelper.stopHelloServerAndWaitUntilUnavailable();
    }

    public void startWorld() throws InterruptedException, IOException {
        TestHelper.startWorldServerAndWaitUntilAvailable();
    }

    public void stopWorld() throws InterruptedException {
        TestHelper.stopWorldServerAndWaitUntilUnavailable();
    }

    public void startFilibuster() throws InterruptedException, IOException {
        TestHelper.startMockFilibusterServerAndWaitUntilAvailable();
    }

    public void stopFilibuster() throws InterruptedException {
        TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    public void startExternalServer() throws InterruptedException, IOException {
        TestHelper.startExternalServerAndWaitUntilAvailable();
    }

    public void stopExternalServer() throws InterruptedException {
        TestHelper.stopExternalServerAndWaitUntilUnavailable();
    }
}