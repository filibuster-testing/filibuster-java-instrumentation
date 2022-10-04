package cloud.filibuster.dei.implementations;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexBase;
import cloud.filibuster.instrumentation.datatypes.Callsite;

import java.util.ArrayList;

import static cloud.filibuster.instrumentation.helpers.Hashing.createDigest;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteHashCallsiteProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteHashIncludedPayloadProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteHashIncludedStackTraceProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteIncludePayloadProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getCallsiteIncludeStackTraceProperty;

public class DistributedExecutionIndexV1 extends DistributedExecutionIndexBase implements DistributedExecutionIndex {
    // TODO: rename properties because they are DEI, not callsite, properties.

    @Override
    public void push(Callsite callsite) {
        ArrayList<String> toStringResult = new ArrayList<>();

        toStringResult.add(callsite.getServiceName());
        toStringResult.add(callsite.getFileName());
        toStringResult.add(callsite.getLineNumber());
        toStringResult.add(callsite.getClassOrModuleName());
        toStringResult.add(callsite.getMethodOrFunctionName());

        // TODO: rename to v1.
        if (getCallsiteIncludePayloadProperty()) {
            if (getCallsiteHashIncludedPayloadProperty()) {
                toStringResult.add(createDigest(callsite.getSerializedArguments()));
            } else {
                toStringResult.add(callsite.getSerializedArguments());
            }
        }

        // TODO: rename to v1.
        if (getCallsiteIncludeStackTraceProperty()) {
            // TODO: remove me.
            if (getCallsiteHashIncludedStackTraceProperty()) {
                toStringResult.add(createDigest(callsite.getSerializedStackTrace()));
            } else {
                toStringResult.add(callsite.getSerializedStackTrace());
            }
        }

        // TODO: rename to v1.
        if (getCallsiteHashCallsiteProperty()) {
            // TODO: rename me.
            push(createDigest(String.join("-", toStringResult)));
        } else {
            push(String.join("-", toStringResult));
        }
    }
}
