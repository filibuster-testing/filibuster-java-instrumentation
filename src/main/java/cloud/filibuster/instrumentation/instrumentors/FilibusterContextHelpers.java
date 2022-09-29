package cloud.filibuster.instrumentation.instrumentors;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexType;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.storage.ContextStorage;

import static cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor.getDistributedExecutionIndexForServiceNameAndRequestId;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterLocks.distributedExecutionIndexLock;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterLocks.vectorClockLock;

public class FilibusterContextHelpers {
    private FilibusterContextHelpers() {

    }

    public static void populateExecutionMapsFromContextStorage(
            String serviceName, ContextStorage contextStorage, String requestId) {
        // Get incoming values.
        VectorClock vectorClock = contextStorage.getVectorClock();
        String distributedExecutionIndexStr = contextStorage.getDistributedExecutionIndex();

        synchronized (distributedExecutionIndexLock) {
            DistributedExecutionIndex distributedExecutionIndexFromContext = getDistributedExecutionIndexForServiceNameAndRequestId(serviceName, requestId, null);

            // If we already have an execution index for this request id, then we just continue
            // using that execution index.
            //
            // If we don't, we try two different things.
            //
            if (distributedExecutionIndexFromContext == null) {

                if (distributedExecutionIndexStr == null) {
                    // If we didn't receive an execution index through context propagation from another service
                    // this means that this is the first time that we are seeing this request.
                    // Therefore, we create a new execution index and assign it to the request id.

                    FilibusterClientInstrumentor.setDistributedExecutionIndexForRequestId(serviceName, requestId, DistributedExecutionIndexType.getImplType().createImpl());
                } else {
                    // If we did receive an execution index through context propagation, we need to deserialize
                    // the execution index and then assign it to that request.

                    DistributedExecutionIndex incomingDistributedExecutionIndex = DistributedExecutionIndexType.getImplType().createImpl().deserialize(contextStorage.getDistributedExecutionIndex());
                    FilibusterClientInstrumentor.setDistributedExecutionIndexForRequestId(serviceName, requestId, incomingDistributedExecutionIndex);
                }
            }
        }

        synchronized (vectorClockLock) {
            if (vectorClock != null) {
                // If we already have a vclock for this request id, but a cycle exists
                // in the graph, we might receive another request for this same request id;
                // in this case, we need to merge the incoming clock with our clock and
                // store back in the mapping.

                VectorClock mergedVectorClock = new VectorClock();
                mergedVectorClock = VectorClock.merge(
                        contextStorage.getVectorClock(),
                        FilibusterClientInstrumentor.getVectorClockForServiceNameAndRequestId(serviceName, requestId, new VectorClock()));
                FilibusterClientInstrumentor.setVectorClockForRequestId(serviceName, requestId, mergedVectorClock);
            } else {
                // Otherwise, we just store in the mapping.
                FilibusterClientInstrumentor.setVectorClockForRequestId(serviceName, requestId, new VectorClock());
            }
        }
    }
}
