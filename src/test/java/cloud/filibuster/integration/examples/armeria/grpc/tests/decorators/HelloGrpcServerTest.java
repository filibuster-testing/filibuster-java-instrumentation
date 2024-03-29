package cloud.filibuster.integration.examples.armeria.grpc.tests.decorators;

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
    public void clearStateFromLastExecution() {
        clearDistributedExecutionIndexForRequestId();
        clearVectorClockForRequestId();
    }

    public static void startHello() throws InterruptedException, IOException {
        startHelloServerAndWaitUntilAvailable();
    }

    public static void stopHello() throws InterruptedException {
        stopHelloServerAndWaitUntilUnavailable();
    }

    public static void startWorld() throws InterruptedException, IOException {
        startWorldServerAndWaitUntilAvailable();
    }

    public static void stopWorld() throws InterruptedException {
        stopWorldServerAndWaitUntilUnavailable();
    }

    public static void startFilibuster() throws InterruptedException, IOException {
        startMockFilibusterServerAndWaitUntilAvailable();
    }

    public static void stopFilibuster() throws InterruptedException {
        stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    public static void startExternalServer() throws InterruptedException, IOException {
        startExternalServerAndWaitUntilAvailable();
    }

    public static void stopExternalServer() throws InterruptedException {
        stopExternalServerAndWaitUntilUnavailable();
    }

    public void waitForWaitComplete() throws InterruptedException {
        // race condition: join() completes at the same point that response.whenComplete() finishes
        // this is where we pop the EI, this could be a potential problem, probably need to enforce a mutex.
        // for now, hack it and sleep.
        Thread.sleep(20);
    }
}