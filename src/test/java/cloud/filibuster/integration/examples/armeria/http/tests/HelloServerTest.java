package cloud.filibuster.integration.examples.armeria.http.tests;

import cloud.filibuster.integration.instrumentation.FilibusterBaseTest;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startMockFilibusterServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;

public class HelloServerTest extends FilibusterBaseTest {
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

    public void startWorldServer() throws InterruptedException, IOException {
        startWorldServerAndWaitUntilAvailable();
    }

    public void stopWorldServer() throws InterruptedException {
        stopWorldServerAndWaitUntilUnavailable();
    }

    public void startHelloServer() throws InterruptedException, IOException {
        startHelloServerAndWaitUntilAvailable();
    }

    public void stopHelloServer() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
    }
}
