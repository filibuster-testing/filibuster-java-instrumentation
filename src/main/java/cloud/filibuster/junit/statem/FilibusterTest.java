package cloud.filibuster.junit.statem;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterTestRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.assertions.Helpers;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static cloud.filibuster.junit.Assertions.getExecutedRPCs;
import static cloud.filibuster.junit.Assertions.getFailedRPCs;
import static cloud.filibuster.junit.Assertions.getFaultsInjected;

public interface FilibusterTest {
    void failureBlock();

    void setupBlock();

    void stubBlock();

    void testBlock();

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
            Helpers.testBlock(this::testBlock);

            // If a fault was injected, ask the developer to specify an alternative assertion block.
            boolean shouldRunAssertionBlock = true;
            JSONObject rpcWhereFirstFaultInjected = rpcWhereFirstFaultInjected();

            if (rpcWhereFirstFaultInjected != null) {
                String requestToString = null;

                if (rpcWhereFirstFaultInjected.has("args")) {
                    JSONObject argsJsonObject = rpcWhereFirstFaultInjected.getJSONObject("args");

                    if (argsJsonObject.has("toString")) {
                        requestToString = argsJsonObject.getString("toString");
                    }
                }

                if (modifiedAssertionsByMethod.containsKey(rpcWhereFirstFaultInjected.getString("method"))) {
                    modifiedAssertionsByMethod.get(rpcWhereFirstFaultInjected.getString("method")).run();
                    shouldRunAssertionBlock = false;
                } else if (requestToString != null && modifiedAssertionsByRequest.containsKey(requestToString)) {
                    modifiedAssertionsByRequest.get(requestToString).run();
                    shouldRunAssertionBlock = false;
                }
                else {
                    throw new FilibusterTestRuntimeException("Test injected a fault, but no specification of failure behavior present.\n");
                }
            }

            if (shouldRunAssertionBlock) {
                // Only in the reference execution, execute the assertion block.
                // This assumes that the assertion block will not pass if the request returns error.
                Helpers.assertionBlock(this::assertTestBlock);
            }

            // Verify stub invocations.
            Helpers.assertionBlock(this::assertStubBlock);
        } catch (StatusRuntimeException statusRuntimeException) {
            // Look up the first RPC where a fault was injected.
            JSONObject rpcWhereFirstFaultInjected = rpcWhereFirstFaultInjected();

            // If we're in the reference execution, just rethrow.
            if (rpcWhereFirstFaultInjected == null) {
                throw statusRuntimeException;
            }

            // See if the developer told us what would happen when this fault was injected.
            if (!expectedExceptions.containsKey(rpcWhereFirstFaultInjected.getString("method"))) {
                // If the user didn't tell us what should happen when this fault was injected,
                // throw an error.
                throw new FilibusterTestRuntimeException(
                        "Test threw an exception, but no specification of failure behavior present.\n" +
                        statusRuntimeException.getMessage());
            } else {
                // Verify the user specified behavior matches the system behavior.
                Map.Entry<Status.Code, String> expectedException = expectedExceptions.get(rpcWhereFirstFaultInjected.getString("method"));

                Status expectedStatus = Status.fromCode(expectedException.getKey()).withDescription(expectedException.getValue());
                Status actualStatus = statusRuntimeException.getStatus();

                // Verify code matches.
                boolean codeMatches = expectedStatus.getCode().equals(actualStatus.getCode());

                // Verify description matches.
                boolean descriptionMatches;

                if (expectedStatus.getDescription() == null && actualStatus.getDescription() != null) {
                    throw new FilibusterTestRuntimeException("Expected exception description is null but actual exception description is NOT null.");
                } else if (expectedStatus.getDescription() != null && actualStatus.getDescription() == null) {
                    throw new FilibusterTestRuntimeException("Expected exception description is NOT null but actual exception description is null.");
                } else if (expectedStatus.getDescription() == null && actualStatus.getDescription() == null) {
                    descriptionMatches = true;
                } else {
                    descriptionMatches = expectedStatus.getDescription().equals(actualStatus.getDescription());
                }

                if (codeMatches && descriptionMatches) {
                    for (Map.Entry<Status.Code, Runnable> adjustedExpectation : adjustedExpectations.entrySet()) {
                        if (adjustedExpectation.getKey().equals(statusRuntimeException.getStatus().getCode())) {
                            adjustedExpectation.getValue().run();
                        }
                    }

                    // Verify stub invocations.
                    Helpers.assertionBlock(this::assertStubBlock);
                } else {
                    throw new FilibusterTestRuntimeException(
                            "Expected exception description does not match the actual thrown exception's description.\n" +
                            "expected: " + expectedStatus + "\n" +
                            "actual: " + actualStatus);
                }
            }
        } finally {
            // Execute teardown.
            Helpers.teardownBlock(this::teardownBlock);

            // Fail the test if any RPCs were left UNIMPLEMENTED (and, we didn't inject it!)
            HashMap<DistributedExecutionIndex, JSONObject> failedRPCs = getFailedRPCs();

            if (getFaultsInjected().size() == 0) {
                for (Map.Entry<DistributedExecutionIndex, JSONObject> failedRPC : failedRPCs.entrySet()) {
                    JSONObject jsonObject = failedRPC.getValue();

                    if (jsonObject.has("exception")) {
                        JSONObject exceptionJsonObject = jsonObject.getJSONObject("exception");

                        if (exceptionJsonObject.has("metadata")) {
                            JSONObject metadataExceptionJsonObject = exceptionJsonObject.getJSONObject("metadata");

                            if (metadataExceptionJsonObject.has("code")) {
                                String code = metadataExceptionJsonObject.getString("code");

                                if (code.equals("UNIMPLEMENTED")) {
                                    throw new FilibusterTestRuntimeException("Some RPCs are left UNIMPLEMENTED!");
                                }
                            }
                        }
                    }
                }
            }

            if (getFaultsInjected().size() == 0) {
                // Fail the test if something hasn't had a verifyThat called on it.
                for (Map.Entry<String, Boolean> verifyThat : GrpcMock.verifyThatMapping.entrySet()) {
                    if (!verifyThat.getValue()) {
                        throw new FilibusterTestRuntimeException("RPC " + verifyThat.getKey() + " has no assertions on invocation count!");
                    }
                }
            }
        }
    }

    // *****************************************************************************************************************
    // Helpers.
    // *****************************************************************************************************************

    HashMap<String, Map.Entry<Status.Code, String>> expectedExceptions = new HashMap<>();

    default void downstreamFailureResultsInException(MethodDescriptor methodDescriptor, Status.Code code, String description) {
        expectedExceptions.put(methodDescriptor.getFullMethodName(), Pair.of(code, description));
    }

    default JSONObject rpcWhereFirstFaultInjected() {
        // Look up the RPCs that were executed and the faults that were injected.
        HashMap<DistributedExecutionIndex, JSONObject> executedRPCs = getExecutedRPCs();
        HashMap<DistributedExecutionIndex, JSONObject> faultsInjected = getFaultsInjected();

        // If no fault was injected, it's gotta be the reference execution, return null;
        if (faultsInjected.size() == 0) {
            return null;
        }

        // Fail if more than one fault was injected.
        // We could be smarter about this, but let's skip it for now since we probably won't run tests this way.
        if (faultsInjected.size() > 1) {
            throw new FilibusterTestRuntimeException(
                    "Test threw an exception; however, multiple faults were injected.");
        }

        // At this point, there should be only a single injected fault.
        // Therefore, find it and return the associated RPC where the fault was injected.
        Map.Entry<DistributedExecutionIndex, JSONObject> firstFaultInjected = null;

        for (Map.Entry<DistributedExecutionIndex, JSONObject> faultInjected : faultsInjected.entrySet()) {
            firstFaultInjected = faultInjected;
            break;
        }

        return executedRPCs.get(firstFaultInjected.getKey());
    }

    HashMap<Status.Code, Runnable> adjustedExpectations = new HashMap<>();

    default void onException(Status.Code code, Runnable runnable) {
        adjustedExpectations.put(code, runnable);
    }

    HashMap<String, Runnable> modifiedAssertionsByMethod = new HashMap<>();

    default void onFaultOnMethod(MethodDescriptor methodDescriptor, Runnable runnable) {
        modifiedAssertionsByMethod.put(methodDescriptor.getFullMethodName(), runnable);
    }

    HashMap<String, Runnable> modifiedAssertionsByRequest = new HashMap<>();

    default  <ReqT> void onFaultOnRequest(MethodDescriptor methodDescriptor, ReqT request, Runnable runnable) {
        modifiedAssertionsByRequest.put(request.toString(), runnable);
    }
}
