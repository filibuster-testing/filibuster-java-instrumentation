package cloud.filibuster.dei.implementations;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexBase;
import cloud.filibuster.instrumentation.datatypes.Callsite;

import java.util.ArrayList;

import static cloud.filibuster.instrumentation.helpers.Hashing.createDigest;
import static cloud.filibuster.instrumentation.helpers.Property.getDEIV1HashCallsiteProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getDEIV1IncludePayloadProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getDEIV1IncludeStackTraceProperty;

public class DistributedExecutionIndexV1 extends DistributedExecutionIndexBase implements DistributedExecutionIndex {
    @Override
    public void push(Callsite callsite) {
        ArrayList<String> toStringResult = new ArrayList<>();

        toStringResult.add(callsite.getServiceName());
        toStringResult.add(callsite.getFileName());
        toStringResult.add(callsite.getLineNumber());
        toStringResult.add(callsite.getClassOrModuleName());
        toStringResult.add(callsite.getMethodOrFunctionName());

        // TODO: add else clauses.
        // TODO: call super class.
        // TODO: use key elements.

        // TODO: rename me.
        if (getDEIV1IncludePayloadProperty()) {
            toStringResult.add(createDigest(callsite.getSerializedArguments()));
        }

        // TODO: rename me.
        if (getDEIV1IncludeStackTraceProperty()) {
            toStringResult.add(createDigest(callsite.getSerializedStackTrace()));
        }

        // TODO: hash only for now.
        // TODO: call superclass.
        // TODO: add prefix for v1: or do it in a version interface.
        // TODO: remove callsite from the name of property
        if (getDEIV1HashCallsiteProperty()) {
            // TODO: rename me.
            push(createDigest(String.join("-", toStringResult)));
        } else {
            push(String.join("-", toStringResult));
        }
    }
}
