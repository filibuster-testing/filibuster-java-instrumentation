package cloud.filibuster.instrumentation.storage;

import cloud.filibuster.instrumentation.datatypes.VectorClock;

import javax.annotation.Nullable;

/**
 * Context storage used for simulation and parameterization of context management to simulate
 * context propagation in OpenTelemetry integration without directly requiring that library.
 */
public interface ContextStorage {

    /**
     * Get request identifier for the current request from the context.
     *
     * @return request id for the current request.
     */
    @Nullable
    String getRequestId();

    /**
     * Get vector clock for the current request from the context.
     *
     * @return vector clock for the current request.
     */
    @Nullable
    VectorClock getVectorClock();

    /**
     * Get incoming vector clock for the current request.
     *
     * @return vector clock for the incoming request that triggered this request.
     */
    @Nullable
    VectorClock getOriginVectorClock();

    /**
     * Get the execution index for the current request.
     *
     * @return execution index for the current request.
     */
    @Nullable
    String getDistributedExecutionIndex();

    /**
     * Set the current request id in the context.
     *
     * @param requestId the identifier for the request.
     */
    void setRequestId(String requestId);

    /**
     * Set the vector clock in the context.
     *
     * @param vectorClock vector clock for the current request.
     */
    void setVectorClock(VectorClock vectorClock);

    /**
     * Set the incoming vector clock in the context.
     *
     * @param originVectorClock vector clock for the incoming request that triggered this request.
     */
    void setOriginVectorClock(VectorClock originVectorClock);

    /**
     * Set the execution index in the context.
     *
     * @param distributedExecutionIndex execution index for the current request.
     */
    void setDistributedExecutionIndex(String distributedExecutionIndex);

}
