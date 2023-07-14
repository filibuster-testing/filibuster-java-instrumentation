package cloud.filibuster.junit.statem;

import cloud.filibuster.instrumentation.datatypes.Pair;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Create a specification for the Filibuster GRPC test interface for multiple faults that are injected simultaneously.
 */
public class CombinedFaultSpecification {
    private final List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> requestFaults;

    public CombinedFaultSpecification(Builder builder){
        this.requestFaults = builder.requestFaults;
    }

    public List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> getRequestFaults() {
        return this.requestFaults;
    }

    /**
     * Builder for creation of a {@link CombinedFaultSpecification}.
     */
    public static class Builder {
        List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> requestFaults = new ArrayList<>();

        /**
         * Add a specific request fault to the {@link CombinedFaultSpecification}.
         *
         * @param methodDescriptor a GRPC method descriptor
         * @param request the request
         * @return {@link Builder}
         */
        @CanIgnoreReturnValue
        public Builder faultOnRequest(
                MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3> methodDescriptor,
                GeneratedMessageV3 request) {
            Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3> fault = Pair.of(methodDescriptor, request);
            requestFaults.add(fault);
            return this;
        }

        public CombinedFaultSpecification build() {
            return new CombinedFaultSpecification(this);
        }
    }
}
