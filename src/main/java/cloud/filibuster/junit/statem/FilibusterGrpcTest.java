package cloud.filibuster.junit.statem;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestInternalRuntimeException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.assertions.Helpers;

import cloud.filibuster.junit.statem.keys.CompositeFaultKey;
import cloud.filibuster.junit.statem.keys.FaultKey;
import cloud.filibuster.junit.statem.keys.SingleFaultKey;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static cloud.filibuster.junit.Assertions.getExecutedRPCs;
import static cloud.filibuster.junit.Assertions.getFailedRPCs;
import static cloud.filibuster.junit.Assertions.getFaultsInjected;
import static cloud.filibuster.junit.statem.keys.CompositeFaultKey.findMatchingFaultKey;

/**
 * Testing interface for writing tests of services that issue GRPCs.  Provides a number of features:
 *
 * <ul>
 * <li>Ensures that setup, test, and teardown are logically separated, making fault injection only active when the test is in progress.</li>
 * <li>Ensures that developers stub all dependencies and indicate precisely which dependencies will be invoked how many times.</li>
 * <li>Forces developers to specifically indicate failure handling behavior for all RPCs issued by the application.</li>
 * <li>Applies compositional reasoning to minimize the number of failure scenarios the user has to specify.</li>
 * </ul>
 *
 * <p>Requires that developers implement this interface and create a test method annotated with
 * {@link cloud.filibuster.junit.TestWithFilibuster @TestWithFilibuster}
 * that just executes the interface function {@link #execute() super.execute()}.</p>
 */
public interface FilibusterGrpcTest {
    AtomicReference<GeneratedMessageV3> response = new AtomicReference<GeneratedMessageV3>();

    /**
     * Set the GRPc response.  For use in {@link #executeTestBlock()}.
     *
     * @param response GRPC response.
     */
    default void setResponse(GeneratedMessageV3 response) {
        this.response.set(response);
    }

    /**
     * Get the GRPC response.  For use in {@link #assertTestBlock()}.
     *
     * @return the GRPC response
     */
    default GeneratedMessageV3 getResponse() {
        return this.response.get();
    }

    /**
     * Test authors should place failure specification in this method.  For example,
     * {@link #assertFaultThrows} to indicate that a method,
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

    // *****************************************************************************************************************
    // Fault API: specify state of the system when exception is thrown.
    // *****************************************************************************************************************

    HashMap<Status.Code, Runnable> errorAssertions = new HashMap<>();

    /**
     * Use of this method informs Filibuster that different assertions will hold true when this error code is
     * returned to the user, as part of a {@link StatusRuntimeException}.  These assertions should be placed in the
     * associated {@link Runnable}.  This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param code the thrown exception's status code when a fault is injected
     * @param runnable assertion block
     */
    default void assertOnException(Status.Code code, Runnable runnable) {
        errorAssertions.put(code, runnable);
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that throw.
    // *****************************************************************************************************************

    HashMap<FaultKey, Map.Entry<Status.Code, String>> faultKeysThatThrow = new HashMap<>();

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param thrownCode the thrown exception's status code when a fault is injected
     * @param thrownMessage the thrown exception's message that is returned when a fault is injected
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor), Pair.of(thrownCode, thrownMessage));
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code the status code of the fault injected
     * @param thrownCode the thrown exception's status code when a fault is injected
     * @param thrownMessage the thrown exception's message that is returned when a fault is injected
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor, code), Pair.of(thrownCode, thrownMessage));
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request the request the fault was injected on
     * @param thrownCode the thrown exception's status code when a fault is injected
     * @param thrownMessage the thrown exception's message that is returned when a fault is injected
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            ReqT request,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor, request), Pair.of(thrownCode, thrownMessage));
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code the status code of the fault injected
     * @param request the request the fault was injected on
     * @param thrownCode the thrown exception's status code when a fault is injected
     * @param thrownMessage the thrown exception's message that is returned when a fault is injected
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            ReqT request,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor, code, request), Pair.of(thrownCode, thrownMessage));
    }

    // *****************************************************************************************************************
    // Fault API: no error handling, should propagate upstream.
    // *****************************************************************************************************************

    List<FaultKey> faultKeysThatPropagate = new ArrayList<>();

    /**
     * Indicate that this RPC endpoint has no error handling, or an error handler that logs and rethrows, and that
     * any faults injected will be propagated directly back to the upstream.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultPropagates(
            MethodDescriptor<ReqT, ResT> methodDescriptor
    ) {
        faultKeysThatPropagate.add(new SingleFaultKey<>(methodDescriptor));
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that have no impact.
    // *****************************************************************************************************************

    List<FaultKey> faultKeysWithNoImpact = new ArrayList<>();

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method will result
     * in the primary assertions, placed in the {@link #assertTestBlock()} continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor
    ) {
        faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor));
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method
     * with this status code will result in the primary assertions, placed in the {@link #assertTestBlock()}
     * continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code the status code
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code
    ) {
        faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor, code));
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method
     * with this request will result in the primary assertions, placed in the {@link #assertTestBlock()}
     * continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request the request
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            ReqT request
    ) {
        faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor, request));
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method
     * with this status code and request will result in the primary assertions, placed in
     * the {@link #assertTestBlock()} continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code the status code
     * @param request the request
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            ReqT request
    ) {
        faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor, code, request));
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that contain different assertions.
    // *****************************************************************************************************************

    HashMap<FaultKey, Runnable> assertionsByFaultKey = new HashMap<>();

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param runnable assertion block
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Runnable runnable
    ) {
        assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor), runnable);
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code the status code of the injected fault
     * @param runnable assertion block
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            Runnable runnable
    ) {
        assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor, code), runnable);
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request the request that the fault was injected on
     * @param runnable assertion block
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            ReqT request,
            Runnable runnable
    ) {
        assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor, request), runnable);
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code the status code of the injected fault
     * @param request the request that the fault was injected on
     * @param runnable assertion block
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            ReqT request,
            Runnable runnable
    ) {
        assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor, code, request), runnable);
    }

    // *****************************************************************************************************************
    // Fault API: adjusted expectations for GRPC endpoints.
    // *****************************************************************************************************************

    AtomicReference<Boolean> insideOfErrorAssertionBlock = new AtomicReference<>(false);

    /**
     * Indicates that this RPC has no side effects and therefore may be called 0 or more times.
     * Can only be used inside an {@link #assertOnException assertOnException} block.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void readOnlyRPC(MethodDescriptor<ReqT, ResT> methodDescriptor) {
        if (insideOfErrorAssertionBlock.get()) {
            GrpcMock.adjustExpectation(methodDescriptor, -1);
        } else {
            throw new FilibusterGrpcTestRuntimeException(
                    "Use of readOnlyRPC not allowed outside of assertOnException(...) block.",
                    "Please rewrite code to specify precise assertions on mock invocations.");
        }
    }

    /**
     * Indicates that this RPC has no side effects and therefore may be called 0 or more times.
     * Can only be used inside an {@link #assertOnException assertOnException} block.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request the request
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void readOnlyRPC(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request) {
        if (insideOfErrorAssertionBlock.get()) {
            GrpcMock.adjustExpectation(methodDescriptor, request, -1);
        } else {
            throw new FilibusterGrpcTestRuntimeException(
                    "Use of readOnlyRPC not allowed outside of assertOnException(...) block.",
                    "Please rewrite code to specify precise assertions on mock invocations.");
        }
    }

    /**
     * Indicates that an RPC has side effects and therefore needs explicit invocation counts.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param count the number of times invoked
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void sideEffectingRPC(MethodDescriptor<ReqT, ResT> methodDescriptor, int count) {
        if (insideOfErrorAssertionBlock.get()) {
            GrpcMock.adjustExpectation(methodDescriptor, count);
        } else {
            throw new FilibusterGrpcTestRuntimeException(
                    "Use of sideEffectingRPC not allowed outside of assertOnException(...) block.",
                    "Please rewrite code to specify precise assertions on mock invocations.");
        }
    }

    /**
     * @param methodDescriptor a GRPC method descriptor
     * @param request the request
     * @param count the number of times invoked
     * @param <ReqT> the request type for this method
     * @param <ResT> the response type for this method
     */
    default <ReqT, ResT> void sideEffectingRPC(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request, int count) {
        if (insideOfErrorAssertionBlock.get()) {
            GrpcMock.adjustExpectation(methodDescriptor, request, count);
        } else {
            throw new FilibusterGrpcTestRuntimeException(
                    "Use of sideEffectingRPC not allowed outside of assertOnException(...) block.",
                    "Please rewrite code to specify precise assertions on mock invocations.");
        }
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that contain different assertions for composite faults
    // *****************************************************************************************************************

    /**
     * Use of this method informs Filibuster that these combined faults will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     * *
     * @param compositeFaultSpecification {@link CompositeFaultSpecification}
     * @param runnable assertion block
     */
    default void assertOnFaults(CompositeFaultSpecification compositeFaultSpecification, Runnable runnable) {
        assertionsByFaultKey.put(new CompositeFaultKey(compositeFaultSpecification), runnable);
    }

    // *****************************************************************************************************************
    // Internal
    // *****************************************************************************************************************

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
                JSONObject faultInjected = faultsInjected.get(executedRPC.getKey());
                JSONObject finalExecutedRPC = executedRPC.getValue();
                if (faultInjected.has("forced_exception")) {
                    JSONObject forcedExceptionObject = faultInjected.getJSONObject("forced_exception");
                    finalExecutedRPC.put("forced_exception", forcedExceptionObject);
                }
                rpcsWhereFaultsInjected.add(finalExecutedRPC);
            }
        }

        return rpcsWhereFaultsInjected;
    }

    default boolean performSingleFaultChecking(JSONObject rpcWhereFaultInjected) {
        boolean shouldRunAssertionBlock = true;
        boolean searchComplete = false;

        // Find matching keys.
        for (FaultKey faultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
            if (!searchComplete) {
                // Iterate each key and see if the user specified something for it.
                if (assertionsByFaultKey.containsKey(faultKey)) {
                    // We want to exit on the first find.
                    searchComplete = true;

                    // As long as we have a match, we skip the normal assertions.
                    shouldRunAssertionBlock = false;

                    // Try to run the runnable and abort if it throws.
                    try {
                        // Run the updated assertions.
                        Runnable runnable = assertionsByFaultKey.get(faultKey);

                        if (runnable != null) {
                            runnable.run();
                        } else {
                            throw new FilibusterGrpcTestInternalRuntimeException("runnable is null: this could indicate a problem!");
                        }
                    } catch (Throwable t) {
                        throw new FilibusterGrpcTestRuntimeException(
                                "Assertions in assertOnFault(...) failed.",
                                "Please adjust assertions in so that the passes.",
                                t);
                    }
                }

                // Otherwise, find out if we should ignore the fault.
                if (faultKeysWithNoImpact.contains(faultKey)) {
                    searchComplete = true;
                    shouldRunAssertionBlock = true;
                }
            }
        }

        if (!searchComplete) {
            throw new FilibusterGrpcTestRuntimeException(
                    "Test injected a fault, but no specification of failure behavior present.",
                    "Please use assertOnFault(...) or assertFaultHasNoImpact(...) to specify assertions under fault.");
        }

        return shouldRunAssertionBlock;
    }

    default boolean performMultipleFaultChecking(List<JSONObject> rpcsWhereFaultsInjected) {
        // Try to see if the user said something about this particular set of failures.
        FaultKey faultKey = findMatchingFaultKey(assertionsByFaultKey, rpcsWhereFaultsInjected);

        if (faultKey != null) {
            // Try to run the runnable and abort if it throws.
            try {
                // Run the updated assertions.
                Runnable runnable = assertionsByFaultKey.get(faultKey);

                if (runnable != null) {
                    runnable.run();
                } else {
                    throw new FilibusterGrpcTestInternalRuntimeException("runnable is null: this could indicate a problem!");
                }
            } catch (Throwable t) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Assertions in assertOnFaults(...) failed.",
                        "Please adjust assertions in assertOnFaults(...) so that test passes.",
                        t);
            }

            // Don't run the normal assertions.
            return false;
        }

        // Otherwise, try to verify compositional-ly.
        Set<JSONObject> methodsWithFaultImpact = new HashSet<>();

        for (JSONObject rpcWhereFaultInjected : rpcsWhereFaultsInjected) {
            boolean found = false;

            for (FaultKey singleFaultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
                if (faultKeysWithNoImpact.contains(singleFaultKey)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                methodsWithFaultImpact.add(rpcWhereFaultInjected);
            }
        }

        if (methodsWithFaultImpact.size() == 0) {
            return true;
        } else if (methodsWithFaultImpact.size() == 1) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            return performSingleFaultChecking(rpcsWhereFaultsInjectedWithImpact.get(0));
        } else if (methodsWithFaultImpact.size() < rpcsWhereFaultsInjected.size()) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            return performMultipleFaultChecking(rpcsWhereFaultsInjectedWithImpact);
        } else {
            throw new FilibusterGrpcTestRuntimeException(
                    "Compositional verification failed due to ambiguous failure handling: each fault introduced has different impact.",
                    "Please write an assertOnFaults(...) for this fault combination with appropriate assertions.");
        }
    }

    default void execute() {
        // For each test execution, clear out the adjusted test expectations.
        GrpcMock.resetAdjustedExpectations();

        // For each test execution, clear out verifyThat mapping.
        GrpcMock.resetVerifyThatMapping();

        // Clear out any user-provided failure handling logic before starting the next execution,
        // as we will set all of this up again when we execute the failureBlock().
        errorAssertions.clear();
        faultKeysThatThrow.clear();
        faultKeysThatPropagate.clear();
        faultKeysWithNoImpact.clear();
        assertionsByFaultKey.clear();

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
                if (rpcsWhereFaultsInjected.size() > 1) {
                    shouldRunAssertionBlock = performMultipleFaultChecking(rpcsWhereFaultsInjected);
                } else {
                    shouldRunAssertionBlock = performSingleFaultChecking(rpcsWhereFaultsInjected.get(0));
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
                                "Assertions in assertTestBlock failed due to multiple faults being injected.",
                                "Please use assertOnFault to update assertions so that they hold under fault.",
                                t
                        );
                    } else {
                        throw new FilibusterGrpcTestRuntimeException(
                                "Assertions in assertTestBlock failed.",
                                "Please adjust assertions in assertTestBlock so that test passes.",
                                t);
                    }
                }
            }

            // Verify stub invocations.
            Helpers.assertionBlock(this::assertStubBlock);
        } catch (StatusRuntimeException statusRuntimeException) {
            // Look up the faults that were injected.
            List<JSONObject> rpcsWhereFaultsInjected = rpcsWhereFaultsInjected();

            if (rpcsWhereFaultsInjected == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("rpcsWhereFaultsInjected is null: this could indicate a problem!");
            }

            // If we're in the reference execution, just rethrow.
            if (rpcsWhereFaultsInjected.size() == 0) {
                throw statusRuntimeException;
            }

            // If we are not in the reference execution.
            if (rpcsWhereFaultsInjected.size() > 1) {
                // TODO: We do not have an example for this yet.
                throw new FilibusterGrpcTestInternalRuntimeException("NOT IMPLEMENTED.");
            } else {
                // Get the only fault injected.
                JSONObject rpcWhereFaultInjected = rpcsWhereFaultsInjected.get(0);

                // Get actual status.
                Status actualStatus = statusRuntimeException.getStatus();

                // Did the user indicate propagation of faults?
                FaultKey faultKeyIndicatingPropagationOfFaults = didUserIndicatePropagationOfFault(rpcWhereFaultInjected);

                if (faultKeyIndicatingPropagationOfFaults != null) {
                    validatePropagationOfFault(rpcWhereFaultInjected, actualStatus);
                }

                // Did the user indicate that this fault will result in exception?
                List<FaultKey> faultKeysIndicatingThrownExceptionFromFault = didUserIndicateThrownExceptionForFault(rpcWhereFaultInjected);

                if (faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
                    validateThrownException(faultKeysIndicatingThrownExceptionFromFault, actualStatus);
                }

                if (faultKeyIndicatingPropagationOfFaults == null && faultKeysIndicatingThrownExceptionFromFault.size() == 0) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Test threw an exception, but no specification of failure behavior present.",
                            "Use assertFaultThrows(...) to specify failure is expected when fault injected on this method, request or code.",
                            statusRuntimeException);
                }

                if (faultKeyIndicatingPropagationOfFaults != null && faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Test indicates both throw and error propagation: too ambiguous.",
                            "Please verify you are only using either assertOnException(...) or assertFaultPropagates(...).");
                }

                // Verify that we have assertion block for thrown exception.
                if (! errorAssertions.containsKey(statusRuntimeException.getStatus().getCode())) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Missing assertion block for Status.Code." + actualStatus.getCode() + " response.",
                            "Please write assertOnException(Status.Code." + actualStatus.getCode() + ", Runnable) for the assertions that should hold under this status code.");
                }

                // Verify that assertion block runs successfully.
                for (Map.Entry<Status.Code, Runnable> errorAssertion : errorAssertions.entrySet()) {
                    if (errorAssertion.getKey().equals(statusRuntimeException.getStatus().getCode())) {
                        try {
                            insideOfErrorAssertionBlock.set(true);
                            errorAssertion.getValue().run();
                        } catch (Throwable t) {
                            throw new FilibusterGrpcTestRuntimeException(
                                    "Assertions for assertOnException failed.",
                                    "Please adjust assertOnException(Status.Code." + actualStatus.getCode() + ", Runnable) for the assertions that should hold under this status code.",
                                    t);
                        } finally {
                            insideOfErrorAssertionBlock.set(false);
                        }
                    }
                }

                // Verify stub invocations.
                try {
                    Helpers.assertionBlock(this::assertStubBlock);
                } catch (Throwable t) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Assertions did not hold under error response.",
                            "Please adjust assertOnException(Status.Code." + actualStatus.getCode() + ", Runnable) for the assertions that should hold under this status code.",
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

            Status.Code statusCode = getStatusCode("exception", jsonObject);

            if (statusCode == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("statusCode is null: this could indicate a problem!");
            }

            String code = statusCode.toString();

            if (code.equals("UNIMPLEMENTED")) {
                boolean faultInjected = faultsInjected.containsKey(distributedExecutionIndex);

                if (!faultInjected) {
                    throw new FilibusterGrpcTestRuntimeException(
                            "Invoked RPCs was left UNIMPLEMENTED.",
                            "Use stubFor to implement stub.");
                }
            }
        }

        // Fail the test if something hasn't had a verifyThat called on it.
        for (Map.Entry<String, Boolean> verifyThat : GrpcMock.getVerifyThatMapping().entrySet()) {
            if (!verifyThat.getValue()) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Stubbed RPC " + verifyThat.getKey() + " has no assertions on invocation count.",
                        "Use verifyThat to specify expected invocation count.");
            }
        }
    }

    @Nullable
    default Status.Code getStatusCode(String exceptionFieldName, JSONObject jsonObject) {
        if (jsonObject.has(exceptionFieldName)) {
            JSONObject exceptionJsonObject = jsonObject.getJSONObject(exceptionFieldName);

            if (exceptionJsonObject.has("metadata")) {
                JSONObject metadataExceptionJsonObject = exceptionJsonObject.getJSONObject("metadata");

                if (metadataExceptionJsonObject.has("code")) {
                    return Status.Code.valueOf(metadataExceptionJsonObject.getString("code"));
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    default FaultKey didUserIndicatePropagationOfFault(JSONObject rpcWhereFaultInjected) {
        FaultKey foundFaultKey = null;

        for (FaultKey faultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
            if (faultKeysThatPropagate.contains(faultKey)) {
                foundFaultKey = faultKey;
                break;
            }
        }

        return foundFaultKey;
    }

    default List<FaultKey> didUserIndicateThrownExceptionForFault(JSONObject rpcWhereFaultInjected) {
        List<FaultKey> matchingFaultKeys = new ArrayList<>();

        for (FaultKey faultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
            if (faultKeysThatThrow.containsKey(faultKey)) {
                matchingFaultKeys.add(faultKey);
                break;
            }
        }

        return matchingFaultKeys;
    }

    default void validatePropagationOfFault(JSONObject rpcWhereFaultInjected, Status actualStatus) {
        Status.Code injectedFaultStatusCode = getStatusCode("forced_exception", rpcWhereFaultInjected);

        if (injectedFaultStatusCode != null) {
            if (!actualStatus.getCode().equals(injectedFaultStatusCode)) {
                throw new FilibusterGrpcTestRuntimeException(
                        "Injected fault's status code was suppressed, but test indicates this should propagate directly upstream.",
                        "Ensure that use of assertFaultPropagates(...) is correct.");
            }
        } else {
            throw new FilibusterGrpcTestInternalRuntimeException("injectedFaultStatusCode is null; this could indicate a problem!");
        }
    }

    default void validateThrownException(List<FaultKey> matchingFaultKeys, Status actualStatus) {
        // Find a status that matches the error code and description.
        boolean foundMatchingExpectedStatus = false;

        for (FaultKey matchingFaultKey : matchingFaultKeys) {
            Map.Entry<Status.Code, String> expectedException = faultKeysThatThrow.get(matchingFaultKey);

            if (expectedException == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("expectedException is null; this could indicate a problem!");
            }

            Status expectedStatus = Status.fromCode(expectedException.getKey()).withDescription(expectedException.getValue());
            boolean codeMatches = expectedStatus.getCode().equals(actualStatus.getCode());
            boolean descriptionMatches = Objects.equals(expectedStatus.getDescription(), actualStatus.getDescription());

            if (codeMatches && descriptionMatches) {
                foundMatchingExpectedStatus = true;
                break;
            }
        }

        // If not found, throw error.
        if (!foundMatchingExpectedStatus) {
            throw new FilibusterGrpcTestRuntimeException(
                    "Failed RPC resulted in exception, but error codes and descriptions did not match.",
                    "Verify assertFaultThrows(...) and thrown exception match.");
        }
    }
}
