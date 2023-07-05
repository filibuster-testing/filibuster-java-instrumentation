package cloud.filibuster.junit.statem;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestInternalRuntimeException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.assertions.Helpers;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static cloud.filibuster.junit.Assertions.getExecutedRPCs;
import static cloud.filibuster.junit.Assertions.getFailedRPCs;
import static cloud.filibuster.junit.Assertions.getFaultsInjected;

public interface FilibusterGrpcTest {
    void failureBlock();

    void setupBlock();

    void stubBlock();

    void executeTestBlock();

    void assertTestBlock();

    void assertStubBlock();

    void teardownBlock();

    default void execute() {
        // For each test execution, clear out the adjusted test expectations.
        GrpcMock.resetAdjustedExpectations();

        // For each test execution, clear out verifyThat mapping.
        GrpcMock.resetVerifyThatMapping();

        // Execute setup blocks.
        Helpers.setupBlock(this::setupBlock);

        // Stub downstream dependencies.
        Helpers.setupBlock(this::stubBlock);

        // Execute failure block.
        Helpers.setupBlock(this::failureBlock);

        try {
            // Execute the test.
            Helpers.testBlock(this::executeTestBlock);

            // If a fault was injected, ask the developer to specify an alternative assertion block.
            boolean shouldRunAssertionBlock = true;
            List<JSONObject> rpcsWhereFaultsInjected = rpcsWhereFaultsInjected();

            if (rpcsWhereFaultsInjected == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("rpcsWhereFaultsInjected is null: this could indicate a problem!");
            }

            if (rpcsWhereFaultsInjected.size() > 0) {
                // Get description of faults injected.
                Map.Entry<String, String> keysForExecutedRPC = generateKeysForExecutedRPCFromJSON(rpcsWhereFaultsInjected);

                if (keysForExecutedRPC == null) {
                    throw new FilibusterGrpcTestInternalRuntimeException("keysForExecutedRPC is null: this could indicate a problem!");
                }

                String methodKey = keysForExecutedRPC.getKey();
                String argsKey = keysForExecutedRPC.getValue();

                if (rpcsWhereFaultsInjected.size() > 1) {
                    // Multiple faults.
                    //
                    // TODO

                    if (argsKey != null && modifiedAssertionsByRequest.containsKey(methodKey + argsKey)) {
                        try {
                            modifiedAssertionsByRequest.get(methodKey + argsKey).run();
                        } catch (Throwable t) {
                            throw new FilibusterGrpcTestRuntimeException(
                                    "Assertions in onFaultOnRequest(" + methodKey + ", ReqT, Runnable) failed.",
                                    "Please adjust assertions in onFaultOnRequests(Array<MethodDescriptors, GeneratedMessageV3>, Runnable) so that test passes.",
                                    t);
                        }

                        shouldRunAssertionBlock = false;
                    }
                } else {
                    // Single fault.
                    //
                    // See if we have special error handling for this fault, then bypass the existing assertion block
                    // in favor of the fault-specific assertion block.
                    //
                    if (argsKey != null && modifiedAssertionsByRequest.containsKey(methodKey + argsKey)) {
                        try {
                            modifiedAssertionsByRequest.get(methodKey + argsKey).run();
                        } catch (Throwable t) {
                            throw new FilibusterGrpcTestRuntimeException(
                                    "Assertions in onFaultOnRequest(" + methodKey + ", ReqT, Runnable) failed.",
                                    "Please adjust assertions in onFaultOnRequest(" + methodKey + ", " + argsKey.replaceAll("\\n", "") + ", Runnable) so that test passes.",
                                    t);
                        }

                        shouldRunAssertionBlock = false;
                    } else if (modifiedAssertionsByMethod.containsKey(methodKey)) {
                        try {
                            modifiedAssertionsByMethod.get(methodKey).run();
                        } catch (Throwable t) {
                            throw new FilibusterGrpcTestRuntimeException(
                                    "Assertions in onFaultOnMethod(" + methodKey + ", Runnable) failed.",
                                    "Please adjust assertions in onFaultOnMethod(" + methodKey + ", Runnable) so that test passes.",
                                    t);
                        }
                        shouldRunAssertionBlock = false;
                    } else {
                        if (!methodsWithNoFaultImpact.contains(methodKey)) {
                            throw new FilibusterGrpcTestRuntimeException(
                                    "Test injected a fault, but no specification of failure behavior present.",
                                    "Please use onFaultOnMethod(MethodDescriptor, Runnable), onFaultOnRequest(MethodDescriptor, ReqT, Runnable), or onFaultOnMethodHasNoEffect(MethodDescriptor) to specify assertions under fault.");
                        }

                        // Otherwise, pass through and run normal assertion blcok.
                    }
                }
            }

            if (shouldRunAssertionBlock) {
                // Only in the reference execution, execute the assertion block.
                // This assumes that the assertion block will not pass if the request returns error.
                try {
                    Helpers.assertionBlock(this::assertTestBlock);
                } catch (Throwable t) {
                    if (rpcsWhereFaultsInjected.size() > 1) {
                        throw new FilibusterGrpcTestRuntimeException(
                                "Assertions in assertTestBlock() failed due to multiple faults being injected.",
                                "Please use onFaultOnRequests(Array<MethodDescriptors, GeneratedMessageV3>, Runnable) to update assertions so that they hold under fault.",
                                t
                        );
                    } else {
                        throw new FilibusterGrpcTestRuntimeException(
                                "Assertions in assertTestBlock() failed.",
                                "Please adjust assertions in assertTestBlock() so that test passes.",
                                t);
                    }
                }
            }

            // Verify stub invocations.
            Helpers.assertionBlock(this::assertStubBlock);
        } catch (StatusRuntimeException statusRuntimeException) {
            // Look up the first RPC where a fault was injected.
            List<JSONObject> rpcsWhereFaultsInjected = rpcsWhereFaultsInjected();

            if (rpcsWhereFaultsInjected == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("rpcsWhereFaultsInjected is null: this could indicate a problem!");
            }

            // If we're in the reference execution, just rethrow.
            if (rpcsWhereFaultsInjected.size() == 0) {
                throw statusRuntimeException;
            }

            // Verify that at least one of the injected faults is expected to result in an exception.
            List<JSONObject> matchingRPCsWhereFaultsInjected = new ArrayList<>();

            for (JSONObject rpcWhereFaultInjected : rpcsWhereFaultsInjected) {
                String method = rpcWhereFaultInjected.getString("method");
                if (expectedExceptions.containsKey(method)) {
                    matchingRPCsWhereFaultsInjected.add(rpcWhereFaultInjected);
                }
            }

            // See if the developer told us what would happen when this fault was injected.
            if (matchingRPCsWhereFaultsInjected.size() == 0) {
                // If the user didn't tell us what should happen when this fault was injected,
                // throw an error.
                throw new FilibusterGrpcTestRuntimeException(
                        "Test threw an exception, but no specification of failure behavior present.",
                        "Use downstreamFailureResultsInException(MethodDescriptor, Status.Code, String) to specify failure is expected when fault injected.",
                        statusRuntimeException);
            } else {
                // Get actual status.
                Status actualStatus = statusRuntimeException.getStatus();

                // Find a status that matches the error code and description.
                JSONObject foundMatchingExpectedStatus = null;

                for (JSONObject matchingRPCWhereFaultsInjected : matchingRPCsWhereFaultsInjected) {
                    String method = matchingRPCWhereFaultsInjected.getString("method");
                    Map.Entry<Status.Code, String> expectedException = expectedExceptions.get(method);
                    Status expectedStatus = Status.fromCode(expectedException.getKey()).withDescription(expectedException.getValue());
                    boolean codeMatches = expectedStatus.getCode().equals(actualStatus.getCode());
                    boolean descriptionMatches = Objects.equals(expectedStatus.getDescription(), actualStatus.getDescription());

                    if (codeMatches && descriptionMatches) {
                        foundMatchingExpectedStatus = matchingRPCWhereFaultsInjected;
                        break;
                    }
                }

                // If not found, throw error.
                if (foundMatchingExpectedStatus == null) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Failed RPC resulted in exception, but error codes and descriptions did not match.",
                            "Verify downstreamFailureResultsInException(MethodDescriptor, Status.Code, String) and thrown exception match.");
                }

                if (! adjustedExpectationsAndAssertions.containsKey(statusRuntimeException.getStatus().getCode())) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Missing assertion block for Status.Code." + actualStatus.getCode() + " response.",
                            "Please write onException(Status.Code." + actualStatus.getCode() + ", Runnable) for the assertions that should hold under this status code.");
                }

                for (Map.Entry<Status.Code, Runnable> adjustedExpectation : adjustedExpectationsAndAssertions.entrySet()) {
                    if (adjustedExpectation.getKey().equals(statusRuntimeException.getStatus().getCode())) {
                        try {
                            adjustedExpectation.getValue().run();
                        } catch (Throwable t) {
                            throw new FilibusterGrpcTestRuntimeException(
                                    "Assertions for onException(Status.Code." + adjustedExpectation.getKey() + ", Runnable) failed.",
                                    "Please adjust onException(Status.Code." + adjustedExpectation.getKey() + ", Runnable) for the assertions that should hold under this status code.",
                                    t);
                        }
                    }
                }

                try {
                    // Verify stub invocations.
                    Helpers.assertionBlock(this::assertStubBlock);
                } catch (Throwable t) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Assertions did not hold under error response.",
                            "Please write onException(Status.Code." + actualStatus.getCode() + ", Runnable) for the assertions that should hold under this status code.",
                            t);
                }
            }
        } finally {
            // Execute teardown.
            Helpers.teardownBlock(this::teardownBlock);
        }

        // Fail the test if any RPCs were left UNIMPLEMENTED (and, we didn't inject it!)
        HashMap<DistributedExecutionIndex, JSONObject> failedRPCs = getFailedRPCs();

        if (failedRPCs == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("failedRPCs is null: this could indicate a problem!");
        }

        HashMap<DistributedExecutionIndex, JSONObject> faultsInjected = getFaultsInjected();

        if (faultsInjected == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("faultsInjected is null: this could indicate a problem!");
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> failedRPC : failedRPCs.entrySet()) {
            DistributedExecutionIndex distributedExecutionIndex = failedRPC.getKey();
            JSONObject jsonObject = failedRPC.getValue();

            if (jsonObject.has("exception")) {
                JSONObject exceptionJsonObject = jsonObject.getJSONObject("exception");

                if (exceptionJsonObject.has("metadata")) {
                    JSONObject metadataExceptionJsonObject = exceptionJsonObject.getJSONObject("metadata");

                    if (metadataExceptionJsonObject.has("code")) {
                        String code = metadataExceptionJsonObject.getString("code");

                        if (code.equals("UNIMPLEMENTED")) {
                            boolean faultInjected = faultsInjected.containsKey(distributedExecutionIndex);

                            if (!faultInjected) {
                                throw new FilibusterGrpcTestRuntimeException(
                                        "Invoked RPCs was left UNIMPLEMENTED.",
                                        "Use stubFor(MethodDescriptor, ReqT, ResT) to implement stub.");
                            }
                        }
                    }
                }
            }
        }

        // Fail the test if something hasn't had a verifyThat called on it.
        for (Map.Entry<String, Boolean> verifyThat : GrpcMock.verifyThatMapping.entrySet()) {
            if (!verifyThat.getValue()) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Stubbed RPC " + verifyThat.getKey() + " has no assertions on invocation count.",
                        "Use verifyThat(MethodDescriptor, ReqT, Count) to specify expected invocation count.");
            }
        }
    }

    // *****************************************************************************************************************
    // Helpers.
    // *****************************************************************************************************************

    HashMap<String, Map.Entry<Status.Code, String>> expectedExceptions = new HashMap<>();

    default <ReqT, ResT> void downstreamFailureResultsInException(MethodDescriptor<ReqT, ResT> methodDescriptor, Status.Code code, String description) {
        expectedExceptions.put(methodDescriptor.getFullMethodName(), Pair.of(code, description));
    }

    default List<JSONObject> rpcsWhereFaultsInjected() {
        List<JSONObject> rpcsWhereFaultsInjected = new ArrayList<>();

        // Look up the RPCs that were executed and the faults that were injected.
        HashMap<DistributedExecutionIndex, JSONObject> executedRPCs = getExecutedRPCs();

        if (executedRPCs == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("executedRPCs is null: this could indicate a problem!");
        }

        HashMap<DistributedExecutionIndex, JSONObject> faultsInjected = getFaultsInjected();

        if (faultsInjected == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("faultsInjected is null: this could indicate a problem!");
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> executedRPC : executedRPCs.entrySet()) {
            if (faultsInjected.containsKey(executedRPC.getKey())) {
                rpcsWhereFaultsInjected.add(executedRPC.getValue());
            }
        }

        return rpcsWhereFaultsInjected;
    }

    HashMap<Status.Code, Runnable> adjustedExpectationsAndAssertions = new HashMap<>();

    default void onException(Status.Code code, Runnable runnable) {
        adjustedExpectationsAndAssertions.put(code, runnable);
    }

    HashMap<String, Runnable> modifiedAssertionsByMethod = new HashMap<>();

    default <ReqT, ResT> void onFaultOnMethod(MethodDescriptor<ReqT, ResT> methodDescriptor, Runnable runnable) {
        modifiedAssertionsByMethod.put(methodDescriptor.getFullMethodName(), runnable);
    }

    HashMap<String, Runnable> modifiedAssertionsByRequest = new HashMap<>();

    default <ReqT, ResT> void onFaultOnRequest(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request, Runnable runnable) {
        modifiedAssertionsByRequest.put(methodDescriptor.getFullMethodName() + request.toString(), runnable);
    }

    default void onFaultOnRequests(
            List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> rpcList,
            Runnable runnable
    ) {
        Map.Entry<String, String> keysForExecutedRPC = generateKeysForExecutedRPCFromMap(rpcList);
        String methodKey = keysForExecutedRPC.getKey();
        String argsKey = keysForExecutedRPC.getValue();
        modifiedAssertionsByRequest.put(methodKey + argsKey, runnable);
    }

    List<String> methodsWithNoFaultImpact = new ArrayList<>();

    default <ReqT, ResT> void onFaultOnMethodHasNoEffect(MethodDescriptor<ReqT, ResT> methodDescriptor) {
        methodsWithNoFaultImpact.add(methodDescriptor.getFullMethodName());
    }

    default Map.Entry<String, String> generateKeysForExecutedRPCFromMap(
            List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> rpcList
    ) {
        StringBuilder methodKey = new StringBuilder();
        StringBuilder argsKey = new StringBuilder();

        for (Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3> rpc : rpcList) {
            methodKey.append(rpc.getKey().getFullMethodName());
            argsKey.append(rpc.getValue().toString());
        }

        return Pair.of(methodKey.toString(), argsKey.toString());
    }

    default Map.Entry<String, String> generateKeysForExecutedRPCFromJSON(List<JSONObject> rpcsWhereFaultsInjected) {
        StringBuilder methodKey = new StringBuilder();
        StringBuilder argsKey = new StringBuilder();

        for (JSONObject rpcWhereFaultsInjected : rpcsWhereFaultsInjected) {
            methodKey.append(rpcWhereFaultsInjected.getString("method"));

            if (rpcWhereFaultsInjected.has("args")) {
                JSONObject argsJsonObject = rpcWhereFaultsInjected.getJSONObject("args");

                if (argsJsonObject.has("toString")) {
                    argsKey.append(argsJsonObject.getString("toString"));
                }

    // Single fault.
    //
    // See if we have special error handling for this fault, then bypass the existing assertion block
    // in favor of the fault-specific assertion block.
    //
    default boolean performSingleFaultChecking(String methodKey, String argsKey) {
        boolean shouldRunAssertionBlock = true;

        if (argsKey != null && modifiedAssertionsByRequest.containsKey(methodKey + argsKey)) {
            try {
                modifiedAssertionsByRequest.get(methodKey + argsKey).run();
            } catch (Throwable t) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Assertions in onFaultOnRequest(" + methodKey + ", ReqT, Runnable) failed.",
                        "Please adjust assertions in onFaultOnRequest(" + methodKey + ", " + argsKey.replaceAll("\\n", "") + ", Runnable) so that test passes.",
                        t);
            }

            shouldRunAssertionBlock = false;
        } else if (modifiedAssertionsByMethod.containsKey(methodKey)) {
            try {
                modifiedAssertionsByMethod.get(methodKey).run();
            } catch (Throwable t) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Assertions in onFaultOnMethod(" + methodKey + ", Runnable) failed.",
                        "Please adjust assertions in onFaultOnMethod(" + methodKey + ", Runnable) so that test passes.",
                        t);
            }
            shouldRunAssertionBlock = false;
        } else {
            if (!methodsWithNoFaultImpact.contains(methodKey)) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Test injected a fault, but no specification of failure behavior present.",
                        "Please use onFaultOnMethod(MethodDescriptor, Runnable), onFaultOnRequest(MethodDescriptor, ReqT, Runnable), or onFaultOnMethodHasNoEffect(MethodDescriptor) to specify assertions under fault.");
            }

            // Otherwise, pass through and run normal assertion blcok.
        }

        return shouldRunAssertionBlock;
    }

        }

        return Pair.of(methodKey.toString(), argsKey.toString());
    }
}
