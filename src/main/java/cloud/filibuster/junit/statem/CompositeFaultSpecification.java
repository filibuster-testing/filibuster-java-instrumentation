package cloud.filibuster.junit.statem;

import cloud.filibuster.junit.statem.keys.SingleFaultKey;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Create a specification for the Filibuster GRPC test interface for multiple faults that are injected simultaneously.
 */
public class CompositeFaultSpecification {
    private final List<SingleFaultKey> faultKeys;

    public CompositeFaultSpecification(Builder builder){
        this.faultKeys = builder.faultKeys;
    }

    /**
     * Get the list of fault keys associated with this composite fault specification.
     *
     * @return list of fault keys
     */
    public List<SingleFaultKey> getFaultKeys() {
        return this.faultKeys;
    }

    /**
     * Builder for creation of a {@link CompositeFaultSpecification}.
     */
    public static class Builder {
        private final List<SingleFaultKey> faultKeys = new ArrayList<>();

        /**
         * Add a specific request fault to the {@link CompositeFaultSpecification}.
         *
         * @param methodDescriptor a GRPC method descriptor
         * @param request the request
         * @return {@link Builder}
         */
        @CanIgnoreReturnValue
        @SuppressWarnings("unchecked")
        public Builder faultOnRequest(
                MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3> methodDescriptor,
                GeneratedMessageV3 request) {
            SingleFaultKey faultKey = new SingleFaultKey(methodDescriptor, request);
            faultKeys.add(faultKey);
            return this;
        }

        /**
         * Add a specific request fault to the {@link CompositeFaultSpecification}.
         *
         * @param methodDescriptor a GRPC method descriptor
         * @param code the injected faults status code
         * @param request the request
         * @return {@link Builder}
         */
        @CanIgnoreReturnValue
        @SuppressWarnings("unchecked")
        public Builder faultOnRequest(
                MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3> methodDescriptor,
                Status.Code code,
                GeneratedMessageV3 request) {
            SingleFaultKey faultKey = new SingleFaultKey(methodDescriptor, code, request);
            faultKeys.add(faultKey);
            return this;
        }

        /**
         * Build a {@link CompositeFaultSpecification}.
         *
         * @return {@link CompositeFaultSpecification}
         */
        public CompositeFaultSpecification build() {
            return new CompositeFaultSpecification(this);
        }
    }
}
