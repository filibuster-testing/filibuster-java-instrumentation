package cloud.filibuster.instrumentation.libraries.armeria.http.tests;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.instrumentation.FilibusterServer;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.HashMap;

import static cloud.filibuster.instrumentation.datatypes.RequestId.generateNewRequestId;

import static cloud.filibuster.instrumentation.TestHelper.startExternalServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.startMockFilibusterServerAndWaitUntilAvailable;
import static cloud.filibuster.instrumentation.TestHelper.stopExternalServerAndWaitUntilUnavailable;
import static cloud.filibuster.instrumentation.TestHelper.stopMockFilibusterServerAndWaitUntilUnavailable;

public class FilibusterDecoratingHttpClientTest extends FilibusterDecoratingHttpTest {
    @BeforeEach
    public void reEnableInstrumentation() {
        FilibusterDecoratingHttpClient.disableServerCommunication = false;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
        FilibusterServer.noNewTestExecution = false;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.shouldInjectStatusCodeFault = false;
        FilibusterServer.emptyExceptionCauseString = false;
        FilibusterServer.emptyExceptionString = false;
        FilibusterServer.shouldReturnNotFounds = false;
        FilibusterServer.shouldNotAbort = false;
    }

    @AfterAll
    public static void resetFilibuster() {
        FilibusterDecoratingHttpClient.disableServerCommunication = false;
        FilibusterDecoratingHttpClient.disableInstrumentation = false;
        FilibusterServer.noNewTestExecution = false;
        FilibusterServer.shouldInjectExceptionFault = false;
        FilibusterServer.shouldInjectStatusCodeFault = false;
        FilibusterServer.emptyExceptionCauseString = false;
        FilibusterServer.emptyExceptionString = false;
        FilibusterServer.shouldReturnNotFounds = false;
        FilibusterServer.shouldNotAbort = false;
    }

    @BeforeEach
    public void clearConfigurationsBefore() {
        FilibusterServer.payloadsReceived.clear();

        FilibusterClientInstrumentor.clearVectorClockForRequestId();
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
    }

    @AfterEach
    public void clearConfigurationsAfter() {
        FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId();
        FilibusterClientInstrumentor.clearVectorClockForRequestId();

        FilibusterServer.payloadsReceived.clear();
    }

    @BeforeEach
    public void contextConfiguration() {
        setInitialVectorClock(new VectorClock());
        setInitialOriginVectorClock(new VectorClock());

        Callsite callsite = new Callsite("service", "class", "moduleName", "deadbeef");
        DistributedExecutionIndex ei = createNewDistributedExecutionIndex();
        ei.push(callsite);
        setInitialDistributedExecutionIndex(ei.toString());

        setInitialRequestId(generateNewRequestId().toString());
    }

    @AfterEach
    public void resetContextConfiguration() {
        resetInitialRequestId();
        resetInitialDistributedExecutionIndex();
        resetInitialOriginVectorClock();
        resetInitialVectorClock();
    }

    public void startFilibuster() throws InterruptedException, IOException {
        startMockFilibusterServerAndWaitUntilAvailable();
    }

    public void stopFilibuster() throws InterruptedException {
        stopMockFilibusterServerAndWaitUntilUnavailable();
    }

    public void waitForWaitComplete() throws InterruptedException {
        // race condition: join() completes at the same point that response.whenComplete() finishes
        // this is where we pop the EI, this could be a potential problem, probably need to enforce a mutex.
        // for now, hack it and sleep.

        // this also impacts waiting for the invocation_complete in certain tests:
        // for example, FilibusterDecoratingHttpClientTest where the invocation_complete record
        // might not be written and race with the join: therefore, wait until we are certain
        // the join has completed and the record has been written.

        // really, need something better than this because it races and in the CI environment
        // takes much longer because of a slower machine with fewer JVM threads.

        if (System.getenv("GITHUB_ACTIONS") != null) {
            Thread.sleep(30);
        } else {
            Thread.sleep(10);
        }
    }

    public void startExternalServer() throws InterruptedException, IOException {
        startExternalServerAndWaitUntilAvailable();
    }

    public void stopExternalServer() throws InterruptedException {
        stopExternalServerAndWaitUntilUnavailable();
    }

    @SuppressWarnings("unchecked")
    public DistributedExecutionIndex getFirstDistributedExecutionIndexFromMapping() {
        HashMap<String, DistributedExecutionIndex> map = (HashMap<String, DistributedExecutionIndex>) FilibusterClientInstrumentor.getDistributedExecutionIndexByRequest().values().toArray()[0];
        return (DistributedExecutionIndex) map.values().toArray()[0];
    }

    @SuppressWarnings("unchecked")
    public VectorClock getFirstVectorClockFromMapping() {
        HashMap<String, VectorClock> map = (HashMap<String, VectorClock>) FilibusterClientInstrumentor.getVectorClocksByRequest().values().toArray()[0];
        return (VectorClock) map.values().toArray()[0];
    }
}
