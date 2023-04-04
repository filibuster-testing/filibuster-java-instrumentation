package cloud.filibuster.integration.instrumentation.libraries.opentelemetry;

import cloud.filibuster.integration.instrumentation.FilibusterBaseTest;
import cloud.filibuster.integration.instrumentation.TestHelper;

import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class OpenTelemetryHelloGrpcServerTest extends FilibusterBaseTest {
    public static void startHello() throws InterruptedException, IOException {
        TestHelper.startHelloServerAndWaitUntilAvailable();
    }

    public static void stopHello() throws InterruptedException {
        TestHelper.stopHelloServerAndWaitUntilUnavailable();
    }

    public static void startWorld() throws InterruptedException, IOException {
        TestHelper.startWorldServerAndWaitUntilAvailable();
    }

    public static void stopWorld() throws InterruptedException {
        TestHelper.stopWorldServerAndWaitUntilUnavailable();
    }

    public static void startFilibuster() throws InterruptedException, IOException {
        TestHelper.startMockFilibusterServerAndWaitUntilAvailable();
    }

    public static void stopFilibuster() throws InterruptedException {
        TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    public static void startExternalServer() throws InterruptedException, IOException {
        TestHelper.startExternalServerAndWaitUntilAvailable();
    }

    public static void stopExternalServer() throws InterruptedException {
        TestHelper.stopExternalServerAndWaitUntilUnavailable();
    }
}