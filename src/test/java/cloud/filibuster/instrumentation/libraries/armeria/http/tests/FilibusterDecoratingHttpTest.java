package cloud.filibuster.instrumentation.libraries.armeria.http.tests;

import cloud.filibuster.instrumentation.FilibusterBaseTest;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;

import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_EXECUTION_INDEX;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_ORIGIN_VCLOCK;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_REQUEST_ID;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_VCLOCK;

public class FilibusterDecoratingHttpTest extends FilibusterBaseTest {
    private VectorClock initialVectorClock;

    public void setInitialVectorClock(VectorClock vc) {
        initialVectorClock = vc;
        ThreadLocalContextStorage.set(FILIBUSTER_VCLOCK, initialVectorClock);
    }

    public void resetInitialVectorClock() {
        ThreadLocalContextStorage.set(FILIBUSTER_VCLOCK, null);
    }

    public VectorClock getInitialVectorClock() {
        return initialVectorClock;
    }

    public void setInitialOriginVectorClock(VectorClock vc) {
        ThreadLocalContextStorage.set(FILIBUSTER_ORIGIN_VCLOCK, vc);
    }

    public void resetInitialOriginVectorClock() {
        ThreadLocalContextStorage.set(FILIBUSTER_ORIGIN_VCLOCK, null);
    }

    private String initialDistributedExecutionIndex;

    public void setInitialDistributedExecutionIndex(String ei) {
        initialDistributedExecutionIndex = ei;
        ThreadLocalContextStorage.set(FILIBUSTER_EXECUTION_INDEX, initialDistributedExecutionIndex);
    }

    public void resetInitialDistributedExecutionIndex() {
        ThreadLocalContextStorage.set(FILIBUSTER_EXECUTION_INDEX, null);
    }

    public String getInitialDistributedExecutionIndex() {
        return initialDistributedExecutionIndex;
    }

    private String initialRequestId;

    public void setInitialRequestId(String requestId) {
        initialRequestId = requestId;
        ThreadLocalContextStorage.set(FILIBUSTER_REQUEST_ID, initialRequestId);
    }

    public void resetInitialRequestId() {
        ThreadLocalContextStorage.set(FILIBUSTER_REQUEST_ID, null);
    }

    public String getInitialRequestId() {
        return initialRequestId;
    }
}
