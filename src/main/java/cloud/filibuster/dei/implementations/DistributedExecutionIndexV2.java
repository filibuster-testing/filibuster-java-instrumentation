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

public class DistributedExecutionIndexV2 extends DistributedExecutionIndexBase implements DistributedExecutionIndex {
    public static class Components {
        /**
         * Compute the RPC signature for an RPC invocation using callsite.
         *
         * @param callsite callsite of the RPC invocation.
         * @param shouldCreateDigest whether a digest should be computed.
         * @return string representation.
         */
        public static String generateRpcSignatureFromCallsite(Callsite callsite, boolean shouldCreateDigest) {
            ArrayList<String> rpcSignatureElements = new ArrayList<>();
            rpcSignatureElements.add(callsite.getClassOrModuleName());
            rpcSignatureElements.add(callsite.getMethodOrFunctionName());
            rpcSignatureElements.add(callsite.getParameterList());

            if (shouldCreateDigest) {
                return createDigest(String.join("-", rpcSignatureElements));
            } else {
                return '[' + String.join(",", rpcSignatureElements) + ']';
            }
        }

        /**
         * Compute the RPC synchronous component for an RPC invocation using callsite: stable under concurrency.
         *
         * @param callsite callsite of the RPC invocation.
         * @param shouldCreateDigest whether a digest should be computed.
         * @return string representation.
         */
        public static String generateRpcSynchronousComponentFromCallsite(Callsite callsite, boolean shouldCreateDigest) {
            ArrayList<String> rpcSynchronousElements = new ArrayList<>();
            rpcSynchronousElements.add(callsite.getFileName());
            rpcSynchronousElements.add(callsite.getLineNumber());
            rpcSynchronousElements.add(callsite.getSerializedStackTrace());

            if (shouldCreateDigest) {
                return createDigest(String.join("-", rpcSynchronousElements));
            } else {
                return '[' + String.join(",", rpcSynchronousElements) + ']';
            }
        }

        /**
         * Compute the RPC asynchronous component for an RPC invocation using callsite: required for scheduling nondeterminism.
         *
         * @param callsite callsite of the RPC invocation.
         * @param shouldCreateDigest whether a digest should be computed.
         * @return string representation.
         */
        public static String generateRpcAsynchronousComponentFromCallsite(Callsite callsite, boolean shouldCreateDigest) {
            ArrayList<String> rpcAsynchronousElements = new ArrayList<>();
            rpcAsynchronousElements.add(callsite.getSerializedArguments());

            if (shouldCreateDigest) {
                return createDigest(String.join("-", rpcAsynchronousElements));
            } else {
                return '[' + String.join(",", rpcAsynchronousElements) + ']';
            }
        }

        /**
         * Compute the DEI modulo the invocation count and invocation path: sig + sync + async.
         *
         * @param callsite callsite of the RPC invocation.
         * @param shouldCreateDigest whether a digest should be computed.
         * @return string representation.
         */
        public static String generateDistributedExecutionIndexKey(Callsite callsite, boolean shouldCreateDigest) {
            ArrayList<String> deiElements = new ArrayList<>();
            deiElements.add(generateRpcSignatureFromCallsite(callsite, shouldCreateDigest));
            deiElements.add(generateRpcSynchronousComponentFromCallsite(callsite, shouldCreateDigest));
            deiElements.add(generateRpcAsynchronousComponentFromCallsite(callsite, shouldCreateDigest));
            return String.join("-", deiElements);
        }
    }

    @Override
    public void push(Callsite callsite) {
        ArrayList<String> deiElements = new ArrayList<>();

        // RPC signature.
        deiElements.add(Components.generateRpcSignatureFromCallsite(callsite, getCallsiteHashCallsiteProperty()));

        // Synchronous component.
        if (getCallsiteIncludeStackTraceProperty()) {
            deiElements.add(Components.generateRpcSynchronousComponentFromCallsite(callsite, getCallsiteHashIncludedStackTraceProperty()));
        }

        // Asynchronous component.
        if (getCallsiteIncludePayloadProperty()) {
            deiElements.add(Components.generateRpcAsynchronousComponentFromCallsite(callsite, getCallsiteHashIncludedPayloadProperty()));
        }

        push(String.join("-", deiElements));
    }
}
