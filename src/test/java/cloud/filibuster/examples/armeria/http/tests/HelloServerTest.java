package cloud.filibuster.examples.armeria.http.tests;

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

public class HelloServerTest extends FilibusterBaseTest {
    @BeforeAll
    public static void configurePrettyDistributedExecutionIndexes() {
        setPrettyDistributedExecutionIndexes();
    }

    @AfterAll
    public static void configureNormalDistributedExecutionIndexes() {
        unsetPrettyDistributedExecutionIndexes();
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
