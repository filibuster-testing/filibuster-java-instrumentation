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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cloud.filibuster.junit.Assertions.getExecutedRPCs;
import static cloud.filibuster.junit.Assertions.getFailedRPCs;
import static cloud.filibuster.junit.Assertions.getFaultsInjected;

public interface FilibusterGrpcTest {
    /**
     * Test authors should place failure specification in this method.  For example,
     * {@link #downstreamFailureResultsInException(MethodDescriptor, Status.Code, String)} to indicate that a method,
     * when failed, will cause the service to return an exception.
     */
    void failureBlock();

    /**
     * Test authors should put code for setup of the test in here.  Use of this block inhibits fault injection for any
     * RPCs issued in this block.  For example, if using the service-under-test's API to stage state for running a test,
     * any downstream dependencies that are invoked as part of execution of the setup block will not be tested for
     * faults.  Thereby, this ensures that faults do not prevent proper staging before execution of the actual test.
     */
    void setupBlock();

    /**
     * Test authors should use this block for stubbing any downstream dependencies that need to be stubbed for the test
     * to pass.  Stubbing of these dependencies should be performed using the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor(MethodDescriptor, ReqT, ResT)}
     * method and should NOT use GrpcMock directly as Filibuster needs to interpose on these mocks for fault injection
     * testing.
     */
    void stubBlock();

    /**
     * Test authors should place test execution code here.  Any downstream RPCs that are invoked as a result of a method
     * invocation inside of this block will be subject to fault injection.  Test authors should store any responses
     * that are required for assertions -- placed in the assertion block {@link #assertTestBlock()} -- in instance
     * variables.
     */
    void executeTestBlock();

    /**
     * Test authors should place test assertions here.  Use of this block inhibits fault injection, which is necessary
     * when using the very API under test to perform assertions.  For example, if your test looks up the user to subscribe
     * them to a service and looks up the user to verify they were subscribed, you only want to inject a fault on the
     * user lookup that's part of subscribing -- not verifying that they did indeed get subscribed.
     */
    void assertTestBlock();

    /**
     * Test authors should use this block for performing assertions on stub invocations.  This should be done using the
     * Filibuster-provided {@link GrpcMock#verifyThat(MethodDescriptor, int)} method and NOT using GrpcMock directly, as
     * Filibuster must interpose on these calls to automatically adjust the expected invocation count when faults are
     * injected.
     */
    void assertStubBlock();

    /**
     * Test authors should use this block for performing test teardown.  Similar to the {@link #setupBlock()} method,
     * fault injection is inhibited for any downstream dependencies that are invoked as part of any method invoked in
     * this block.
     */
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
                    shouldRunAssertionBlock = performMultipleFaultChecking(rpcsWhereFaultsInjected, methodKey, argsKey);
                } else {
                    shouldRunAssertionBlock = performSingleFaultChecking(methodKey, argsKey);
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
    // Fault API
    // *****************************************************************************************************************

    default <ReqT, ResT> void downstreamFailureResultsInException(MethodDescriptor<ReqT, ResT> methodDescriptor, Status.Code code, String description) {
        expectedExceptions.put(methodDescriptor.getFullMethodName(), Pair.of(code, description));
    }

    default void onException(Status.Code code, Runnable runnable) {
        adjustedExpectationsAndAssertions.put(code, runnable);
    }

    default <ReqT, ResT> void onFaultOnMethod(MethodDescriptor<ReqT, ResT> methodDescriptor, Runnable runnable) {
        modifiedAssertionsByMethod.put(methodDescriptor.getFullMethodName(), runnable);
    }

    default <ReqT, ResT> void onFaultOnMethodHasNoEffect(MethodDescriptor<ReqT, ResT> methodDescriptor) {
        methodsWithNoFaultImpact.add(methodDescriptor.getFullMethodName());
    }

    default <ReqT, ResT> void onFaultOnRequest(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request, Runnable runnable) {
        modifiedAssertionsByRequest.put(methodDescriptor.getFullMethodName() + request.toString(), runnable);
    }

    default void onFaultOnRequests(List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> rpcList, Runnable runnable) {
        Map.Entry<String, String> keysForExecutedRPC = generateKeysForExecutedRPCFromMap(rpcList);
        String methodKey = keysForExecutedRPC.getKey();
        String argsKey = keysForExecutedRPC.getValue();
        modifiedAssertionsByRequest.put(methodKey + argsKey, runnable);
    }

    // *****************************************************************************************************************
    // Internal
    // *****************************************************************************************************************

    HashMap<String, Map.Entry<Status.Code, String>> expectedExceptions = new HashMap<>();

    HashMap<Status.Code, Runnable> adjustedExpectationsAndAssertions = new HashMap<>();

    HashMap<String, Runnable> modifiedAssertionsByMethod = new HashMap<>();

    HashMap<String, Runnable> modifiedAssertionsByRequest = new HashMap<>();

    List<String> methodsWithNoFaultImpact = new ArrayList<>();

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

    default Map.Entry<String, String> generateKeys(List<Map.Entry<String, String>> keyData) {
        List<String> methodValues = new ArrayList<>();
        List<String> argValues = new ArrayList<>();

        for (Map.Entry<String, String> keyDataItem : keyData) {
            methodValues.add(keyDataItem.getKey());
            argValues.add(keyDataItem.getValue());
        }

        Collections.sort(methodValues);
        Collections.sort(argValues);

        String methodValue = String.join("", methodValues);
        String argsValue = String.join("", argValues);

        return Pair.of(methodValue, argsValue);
    }

    default Map.Entry<String, String> generateKeysForExecutedRPCFromMap(List<Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3>> rpcList) {
        List<Map.Entry<String, String>> keyEntries = new ArrayList<>();

        for (Map.Entry<MethodDescriptor<? extends GeneratedMessageV3, ? extends GeneratedMessageV3>, ? extends GeneratedMessageV3> rpc : rpcList) {
            keyEntries.add(Pair.of(rpc.getKey().getFullMethodName(), rpc.getValue().toString()));
        }

        return generateKeys(keyEntries);
    }

    default Map.Entry<String, String> generateKeysForExecutedRPCFromJSON(List<JSONObject> rpcsWhereFaultsInjected) {
        List<Map.Entry<String, String>> keyEntries = new ArrayList<>();

        for (JSONObject rpcWhereFaultsInjected : rpcsWhereFaultsInjected) {
            if (rpcWhereFaultsInjected.has("args")) {
                JSONObject argsJsonObject = rpcWhereFaultsInjected.getJSONObject("args");

                if (argsJsonObject.has("toString")) {
                    keyEntries.add(Pair.of(rpcWhereFaultsInjected.getString("method"), argsJsonObject.getString("toString")));
                } else {
                    keyEntries.add(Pair.of(rpcWhereFaultsInjected.getString("method"), ""));
                }
            } else {
                keyEntries.add(Pair.of(rpcWhereFaultsInjected.getString("method"), ""));
            }
        }

        return generateKeys(keyEntries);
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

            // Otherwise, pass through and run normal assertion block.
        }

        return shouldRunAssertionBlock;
    }

    // Multiple faults.
    //
    // Look to see if the user specified behavior for that precise combination of faults.
    // If they didn't, try to verify compositionally using the information that they have provided.
    //
    default boolean performMultipleFaultChecking(List<JSONObject> rpcsWhereFaultsInjected, String methodKey, String argsKey) {
        // If the user specified an assertion block for this precise combination of faults.
        if (argsKey != null && modifiedAssertionsByRequest.containsKey(methodKey + argsKey)) {
            try {
                modifiedAssertionsByRequest.get(methodKey + argsKey).run();
            } catch (Throwable t) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Assertions in onFaultOnRequest(Array<MethodDescriptors, GeneratedMessageV3>, ReqT, Runnable) failed.",
                        "Please adjust assertions in onFaultOnRequests(Array<MethodDescriptors, GeneratedMessageV3>, Runnable) so that test passes.",
                        t);
            }

            return false;
        }

        // Otherwise, try to verify compositional-ly.
        Set<JSONObject> methodsWithFaultImpact = new HashSet<>();

        for (JSONObject rpcWhereFaultInjected : rpcsWhereFaultsInjected) {
            String method = rpcWhereFaultInjected.getString("method");

            if (!methodsWithNoFaultImpact.contains(method)) {
                methodsWithFaultImpact.add(rpcWhereFaultInjected);
            }
        }

        if (methodsWithFaultImpact.size() == 0) {
            return true;
        } else if (methodsWithFaultImpact.size() == 1) {
            ArrayList<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            Map.Entry<String, String> keysForExecutedRPC = generateKeysForExecutedRPCFromJSON(rpcsWhereFaultsInjectedWithImpact);

            if (keysForExecutedRPC == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("keysForExecutedRPC is null: this could indicate a problem!");
            }

            String singleFaultMethodKey = keysForExecutedRPC.getKey();
            String singleFaultArgsKey = keysForExecutedRPC.getValue();

            return performSingleFaultChecking(singleFaultMethodKey, singleFaultArgsKey);
        } else if (methodsWithFaultImpact.size() < rpcsWhereFaultsInjected.size()) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            Map.Entry<String, String> keysForExecutedRPC = generateKeysForExecutedRPCFromJSON(rpcsWhereFaultsInjectedWithImpact);

            String methodsWithFaultImpactMethodKey = keysForExecutedRPC.getKey();
            String methodsWithFaultImpactArgsKey = keysForExecutedRPC.getValue();

            return performMultipleFaultChecking(rpcsWhereFaultsInjectedWithImpact, methodsWithFaultImpactMethodKey, methodsWithFaultImpactArgsKey);
        } else {
            throw new FilibusterGrpcTestRuntimeException(
                    "Compositional verification failed due to ambiguous failure handling: each fault introduced has different impact.",
                    "Please write an onFaultOnRequests(Array<MethodDescriptors, GeneratedMessageV3>, Runnable) for this fault combination with appropriate assertions.");
        }
    }
}
