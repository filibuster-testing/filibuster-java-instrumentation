package cloud.filibuster.integration.examples.armeria.grpc.tests.interceptors;

import cloud.filibuster.integration.instrumentation.FilibusterBaseTest;

import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static cloud.filibuster.integration.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startMockFilibusterServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startHelloServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.startWorldServerAndWaitUntilAvailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopHelloServerAndWaitUntilUnavailable;
import static cloud.filibuster.integration.instrumentation.TestHelper.stopWorldServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.clearVectorClockForRequestId;

public class HelloGrpcServerTest extends FilibusterBaseTest {
    @BeforeEach
    protected void clearStateFromLastExecution() {
        clearDistributedExecutionIndexForRequestId();
        clearVectorClockForRequestId();
    }

    protected void startHello() throws InterruptedException, IOException {
        startHelloServerAndWaitUntilAvailable();
    }

    protected void stopHello() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
    }

    protected void startWorld() throws InterruptedException, IOException {
        startWorldServerAndWaitUntilAvailable();
    }

    protected void stopWorld() throws InterruptedException {
        stopWorldServerAndWaitUntilUnavailable();
    }

    protected void startFilibuster() throws InterruptedException, IOException {
        startMockFilibusterServerAndWaitUntilAvailable();
    }

    protected void stopFilibuster() throws InterruptedException {
        stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    protected void startExternalServer() throws InterruptedException, IOException {
        startExternalServerAndWaitUntilAvailable();
    }

    protected void stopExternalServer() throws InterruptedException {
        stopExternalServerAndWaitUntilUnavailable();
    }
}