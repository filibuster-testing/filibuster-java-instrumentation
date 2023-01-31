package cloud.filibuster.integration.instrumentation.libraries.opentelemetry;

import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import io.opentelemetry.context.Context;
import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryContextStorageConstants.EXECUTION_INDEX_KEY;
import static cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryContextStorageConstants.ORIGIN_VCLOCK_KEY;
import static cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryContextStorageConstants.REQUEST_ID_KEY;
import static cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryContextStorageConstants.VCLOCK_KEY;

public class OpenTelemetryContextStorage implements ContextStorage {
    private static final Logger logger = Logger.getLogger(OpenTelemetryContextStorage.class.getName());

    private Context context;

    // Context.current() should maybe be cached, who knows?

    public OpenTelemetryContextStorage() {
        this.context = Context.current();
    }

    public Context getContext() {
        return this.context;
    }

    @Override
    @Nullable
    public String getRequestId() {
        return Context.current().get(REQUEST_ID_KEY);
    }

    @Override
    @Nullable
    public VectorClock getVectorClock() {
        String vectorClockStr = Context.current().get(VCLOCK_KEY);

        VectorClock newVclock = new VectorClock();

        if (vectorClockStr != null) {
            newVclock.fromString(vectorClockStr);
        }

        return newVclock;
    }

    @Override
    @Nullable
    public VectorClock getOriginVectorClock() {
        String originVectorClockStr = Context.current().get(ORIGIN_VCLOCK_KEY);

        VectorClock newVclock = new VectorClock();

        if (originVectorClockStr != null) {
            newVclock.fromString(originVectorClockStr);
        }

        return newVclock;
    }

    @Override
    @Nullable
    public String getDistributedExecutionIndex() {
        return Context.current().get(EXECUTION_INDEX_KEY);
    }

    @Override
    public void setRequestId(String requestId) {
        this.context = this.context.with(REQUEST_ID_KEY, requestId);
        logger.log(Level.SEVERE, "setRequestId: " + requestId);
    }

    @Override
    public void setVectorClock(VectorClock vectorClock) {
        this.context = this.context.with(VCLOCK_KEY, vectorClock.toString());
        logger.log(Level.SEVERE, "setVectorClock: " + vectorClock);
    }

    @Override
    public void setOriginVectorClock(VectorClock originVectorClock) {
        this.context = this.context.with(ORIGIN_VCLOCK_KEY, originVectorClock.toString());
        logger.log(Level.SEVERE, "setOriginVectorClock: " + originVectorClock);
    }

    @Override
    public void setDistributedExecutionIndex(String distributedExecutionIndex) {
        this.context = this.context.with(EXECUTION_INDEX_KEY, distributedExecutionIndex);
        logger.log(Level.SEVERE, "setDistributedExecutionIndex: " + distributedExecutionIndex);
    }
}
